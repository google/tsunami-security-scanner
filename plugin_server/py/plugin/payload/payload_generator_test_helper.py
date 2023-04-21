"""Payload Generator Test Helper."""
from tsunami.proto import payload_generator_pb2 as pg


LINUX_REFLECTIVE_RCE_CONFIG = pg.PayloadGeneratorConfig(
    vulnerability_type=pg.PayloadGeneratorConfig.VulnerabilityType.REFLECTIVE_RCE,
    interpretation_environment=pg.PayloadGeneratorConfig.InterpretationEnvironment.LINUX_SHELL,
    execution_environment=pg.PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT,
)

LINUX_UNSPECIFIED_CONFIG = pg.PayloadGeneratorConfig(
    vulnerability_type=pg.PayloadGeneratorConfig.VulnerabilityType.VULNERABILITY_TYPE_UNSPECIFIED,
    interpretation_environment=pg.PayloadGeneratorConfig.InterpretationEnvironment.LINUX_SHELL,
    execution_environment=pg.PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT,
)

ANY_SSRF_CONFIG = pg.PayloadGeneratorConfig(
    vulnerability_type=pg.PayloadGeneratorConfig.VulnerabilityType.SSRF,
    interpretation_environment=pg.PayloadGeneratorConfig.InterpretationEnvironment.INTERPRETATION_ANY,
    execution_environment=pg.PayloadGeneratorConfig.ExecutionEnvironment.EXEC_ANY,
)

JAVA_REFLECTIVE_RCE_CONFIG = pg.PayloadGeneratorConfig(
    vulnerability_type=pg.PayloadGeneratorConfig.VulnerabilityType.REFLECTIVE_RCE,
    interpretation_environment=pg.PayloadGeneratorConfig.InterpretationEnvironment.JAVA,
    execution_environment=pg.PayloadGeneratorConfig.ExecutionEnvironment.EXEC_INTERPRETATION_ENVIRONMENT,
)
