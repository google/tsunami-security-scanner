"""Tests for google3.third_party.java_src.tsunami.plugin_server.py.common.net.http_status."""

from absl.testing import absltest
from absl.testing import parameterized
from tsunami.plugin_server.py.common.net.http import http_status

HttpStatus = http_status.HttpStatus


class HttpStatusTest(parameterized.TestCase):
  def test_from_code_with_status_code_returns_http_status(self):
    status = HttpStatus.from_code(200)
    self.assertEqual(status, HttpStatus.OK)

  def test_from_code_with_invalid_status_code_returns_http_status_unspecified(
      self):
    status = HttpStatus.from_code(1)
    self.assertEqual(status, HttpStatus.HTTP_STATUS_UNSPECIFIED)

  @parameterized.named_parameters(
      ('with_multiple_choices', HttpStatus.MULTIPLE_CHOICES),
      ('with_moved_permanently', HttpStatus.MOVED_PERMANENTLY),
      ('with_found', HttpStatus.FOUND),
      ('with_see_other', HttpStatus.SEE_OTHER),
      ('with_temporary_redirect', HttpStatus.TEMPORARY_REDIRECT),
      ('with_permanent_redirect', HttpStatus.PERMANENT_REDIRECT))
  def test_is_redirect_returns_true(self, status):
    self.assertTrue(status.is_redirect())

  def test_is_redirect_with_non_redirected_status_returns_false(self):
    self.assertFalse(HttpStatus.LOCKED.is_redirect())

  def test_is_success_with_code_between_199_and_300(self):
    self.assertTrue(HttpStatus.from_code(200).is_success())
    self.assertTrue(HttpStatus.from_code(207).is_success())
    self.assertFalse(HttpStatus.from_code(300).is_success())

  def test___str__returns_status_message(self):
    self.assertEqual(HttpStatus.CONTINUE.__str__(), 'Continue')

if __name__ == '__main__':
  absltest.main()
