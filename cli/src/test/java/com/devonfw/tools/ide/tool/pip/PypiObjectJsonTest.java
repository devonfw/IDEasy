package com.devonfw.tools.ide.tool.pip;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test of {@link PypiObjectJsonDeserializer} and {@link PypiObjectJsonSerializer}.
 */
public class PypiObjectJsonTest extends Assertions {

  private static final String JSON = """
      {
        "last_serial": 32003461,
        "releases": {
          "0.2": [
            {
              "comment_text": "",
              "digests": {
                "blake2b_256": "3d9d1e313763bdfb6a48977b65829c6ce2a43eaae29ea2f907c8bbef024a7219",
                "md5": "9eda07c8be7105aa774c7eb51c023294",
                "sha256": "88bb8d029e1bf4acd0e04d300104b7440086f94cc1ce1c5c3c31e3293aee1f81"
              },
              "downloads": -1,
              "filename": "pip-0.2.tar.gz",
              "has_sig": false,
              "md5_digest": "9eda07c8be7105aa774c7eb51c023294",
              "packagetype": "sdist",
              "python_version": "source",
              "requires_python": null,
              "size": 38734,
              "upload_time": "2008-10-28T17:22:10",
              "upload_time_iso_8601": "2008-10-28T17:22:10Z",
              "url": "https://files.pythonhosted.org/packages/3d/9d/1e313763bdfb6a48977b65829c6ce2a43eaae29ea2f907c8bbef024a7219/pip-0.2.tar.gz",
              "yanked": false,
              "yanked_reason": null
            }
          ],
          "10.0.1": [
            {
              "comment_text": "",
              "digests": {
                "blake2b_256": "0f74ecd13431bcc456ed390b44c8a6e917c1820365cbebcb6a8974d1cd045ab4",
                "md5": "eb92c86bfda9cde5e082a1fd76f1e627",
                "sha256": "717cdffb2833be8409433a93746744b59505f42146e8d37de6c62b430e25d6d7"
              },
              "downloads": -1,
              "filename": "pip-10.0.1-py2.py3-none-any.whl",
              "has_sig": false,
              "md5_digest": "eb92c86bfda9cde5e082a1fd76f1e627",
              "packagetype": "bdist_wheel",
              "python_version": "py2.py3",
              "requires_python": ">=2.7,!=3.0.*,!=3.1.*,!=3.2.*",
              "size": 1307639,
              "upload_time": "2018-04-19T18:56:05",
              "upload_time_iso_8601": "2018-04-19T18:56:05.963596Z",
              "url": "https://files.pythonhosted.org/packages/0f/74/ecd13431bcc456ed390b44c8a6e917c1820365cbebcb6a8974d1cd045ab4/pip-10.0.1-py2.py3-none-any.whl",
              "yanked": false,
              "yanked_reason": null
            },
            {
              "comment_text": "",
              "digests": {
                "blake2b_256": "aee82340d46ecadb1692a1e455f13f75e596d4eab3d11a57446f08259dee8f02",
                "md5": "83a177756e2c801d0b3a6f7b0d4f3f7e",
                "sha256": "f2bd08e0cd1b06e10218feaf6fef299f473ba706582eb3bd9d52203fdbd7ee68"
              },
              "downloads": -1,
              "filename": "pip-10.0.1.tar.gz",
              "has_sig": false,
              "md5_digest": "83a177756e2c801d0b3a6f7b0d4f3f7e",
              "packagetype": "sdist",
              "python_version": "source",
              "requires_python": ">=2.7,!=3.0.*,!=3.1.*,!=3.2.*",
              "size": 1246072,
              "upload_time": "2018-04-19T18:56:09",
              "upload_time_iso_8601": "2018-04-19T18:56:09.474691Z",
              "url": "https://files.pythonhosted.org/packages/ae/e8/2340d46ecadb1692a1e455f13f75e596d4eab3d11a57446f08259dee8f02/pip-10.0.1.tar.gz",
              "yanked": false,
              "yanked_reason": null
            }
          ],
          "25.3": [
            {
              "comment_text": null,
              "digests": {
                "blake2b_256": "443cd717024885424591d5376220b5e836c2d5293ce2011523c9de23ff7bf068",
                "md5": "4bbbf9f0745c4117c8ecc77c561ef74b",
                "sha256": "9655943313a94722b7774661c21049070f6bbb0a1516bf02f7c8d5d9201514cd"
              },
              "downloads": -1,
              "filename": "pip-25.3-py3-none-any.whl",
              "has_sig": false,
              "md5_digest": "4bbbf9f0745c4117c8ecc77c561ef74b",
              "packagetype": "bdist_wheel",
              "python_version": "py3",
              "requires_python": ">=3.9",
              "size": 1778622,
              "upload_time": "2025-10-25T00:55:39",
              "upload_time_iso_8601": "2025-10-25T00:55:39.247292Z",
              "url": "https://files.pythonhosted.org/packages/44/3c/d717024885424591d5376220b5e836c2d5293ce2011523c9de23ff7bf068/pip-25.3-py3-none-any.whl",
              "yanked": false,
              "yanked_reason": null
            },
            {
              "comment_text": null,
              "digests": {
                "blake2b_256": "fe6e74a3f0179a4a73a53d66ce57fdb4de0080a8baa1de0063de206d6167acc2",
                "md5": "31f52f428372020b80cd7518862fbbee",
                "sha256": "8d0538dbbd7babbd207f261ed969c65de439f6bc9e5dbd3b3b9a77f25d95f343"
              },
              "downloads": -1,
              "filename": "pip-25.3.tar.gz",
              "has_sig": false,
              "md5_digest": "31f52f428372020b80cd7518862fbbee",
              "packagetype": "sdist",
              "python_version": "source",
              "requires_python": ">=3.9",
              "size": 1803014,
              "upload_time": "2025-10-25T00:55:41",
              "upload_time_iso_8601": "2025-10-25T00:55:41.394958Z",
              "url": "https://files.pythonhosted.org/packages/fe/6e/74a3f0179a4a73a53d66ce57fdb4de0080a8baa1de0063de206d6167acc2/pip-25.3.tar.gz",
              "yanked": false,
              "yanked_reason": null
            }
          ]
        },
        "vulnerabilities": []
      }
      """;

  private static final String JSON_NORMALIZED = """
      {
        "releases": {
          "0.2": [],
          "10.0.1": [],
          "25.3": []
        }
      }""";

  /**
   * Test of {@link PypiObjectJsonDeserializer}.
   *
   * @throws Exception on error.
   */
  @Test
  public void testDeserialize() throws Exception {
    // arrange
    ObjectMapper mapper = JsonMapping.create();

    // act
    PypiObject pypiObject = mapper.readValue(JSON, PypiObject.class);

    // assert
    assertThat(pypiObject.releases()).containsExactly(VersionIdentifier.of("0.2"), VersionIdentifier.of("10.0.1"), VersionIdentifier.of("25.3"));

    // act
    String json = mapper.writeValueAsString(pypiObject);

    // assert
    assertThat(json).isEqualTo(JSON_NORMALIZED);
  }

}
