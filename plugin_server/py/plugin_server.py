# Copyright 2022 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Main gRPC server to execute Python Tsunami plugins."""
from concurrent import futures
import importlib
import pkgutil
import signal
import threading
import types

from absl import app
from absl import flags
from absl import logging
import grpc
from grpc_health.v1 import health
from grpc_health.v1 import health_pb2
from grpc_health.v1 import health_pb2_grpc
from grpc_reflection.v1alpha import reflection

from tsunami.plugin_server.py import plugin_service
from tsunami.plugin_server.py import tsunami_plugin
from tsunami.proto import plugin_service_pb2
from tsunami.proto import plugin_service_pb2_grpc

_HOST = 'localhost'

_PORT = flags.DEFINE_integer('port', 34567, 'port to listen on')
_THREADS = flags.DEFINE_integer('threads', 10,
                                'number of worker threads in thread pool')
_OUTPUT = flags.DEFINE_string('log_output', '/tmp',
                              'server execution log directory')


def main(unused_argv):
  logging.use_absl_handler()
  logging.get_absl_handler().use_absl_log_file('py_plugin_server',
                                               _OUTPUT.value)
  logging.set_verbosity(logging.INFO)
  # Load plugins from tsunami_plugins repository.
  plugin_pkg = importlib.import_module(
      'py_plugins')
  _import_py_plugins(plugin_pkg)

  server_addr = f'{_HOST}:{_PORT.value}'
  server = grpc.server(futures.ThreadPoolExecutor(max_workers=_THREADS.value))

  _configure_plugin_service(server)
  _configure_health_service(server)
  server.add_secure_port(server_addr, grpc.local_server_credentials())

  server.start()
  logging.info('Server started at %s.', server_addr)

  # Java Process.destroy() sends SIGTERM
  sig_term_received = threading.Event()

  def on_sigterm(signum, frame):
    logging.info('Got signal %s, %s', signum, frame)
    sig_term_received.set()

  signal.signal(signal.SIGTERM, on_sigterm)
  sig_term_received.wait()
  logging.info('Stopped RPC server, Waiting for RPCs to complete...')
  server.stop(3).wait()
  logging.info('Done stopping server')


def _import_py_plugins(plugin_pkg: types.ModuleType):
  """Imports all Python Tsunami plugin modules."""
  for _, name, is_pkg in pkgutil.walk_packages(plugin_pkg.__path__):
    full_name = plugin_pkg.__name__ + '.' + name
    pkg = importlib.import_module(full_name)
    if is_pkg:
      _import_py_plugins(pkg)
    else:
      logging.info('Loaded plugin module %s', full_name)


def _configure_plugin_service(server):
  """Configures the main plugin service for handling plugin related gRPC requests."""
  # Get all VulnDetector class implementations.
  plugins = [cls() for cls in tsunami_plugin.VulnDetector.__subclasses__()]
  servicer = plugin_service.PluginServiceServicer(
      py_plugins=plugins, max_workers=_THREADS.value)
  plugin_service_pb2_grpc.add_PluginServiceServicer_to_server(servicer, server)


def _configure_health_service(server):
  """Configures gRPC health checking service for server health monitoring."""
  health_servicer = health.HealthServicer(
      experimental_non_blocking=True,
      experimental_thread_pool=futures.ThreadPoolExecutor(
          max_workers=_THREADS.value))
  health_pb2_grpc.add_HealthServicer_to_server(health_servicer, server)

  # Set all services to SERVING.
  services = tuple(service.full_name
                   for service in plugin_service_pb2.DESCRIPTOR.services_by_name
                   .values()) + (reflection.SERVICE_NAME, health.SERVICE_NAME)
  for service in services:
    logging.info('Registering service %s', service)
    health_servicer.set(service, health_pb2.HealthCheckResponse.SERVING)
  reflection.enable_server_reflection(services, server)


if __name__ == '__main__':
  app.run(main)
