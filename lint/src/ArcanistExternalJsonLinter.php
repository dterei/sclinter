<?php

/**
 * Simple glue linter which passes the list of paths to a script
 * and parses lint violations emitted by the script in JSON format.
 *
 * Configure this linter by setting these keys in your .arclint section:
 *
 *  - `external-json.script` Script command to run. This can be
 *    the path to a linter script, but may also include flags or use shell
 *    features (see below for examples).
 *
 * The script will be invoked from the project root, so you can specify a
 * relative path like `scripts/lint.sh` or an absolute path like
 * `/opt/lint/lint.sh`.
 *
 * This linter is necessarily more limited in its capabilities than a normal
 * linter which can perform custom processing, but may be somewhat simpler to
 * configure.
 *
 * == Script and JSON format ==
 *
 * The script will be invoked once and list of files to be linted will be passed,
 * as arguments to it. The file may begin with a "-"; ensure
 * your script will not interpret such files as flags (perhaps by ending your
 * script configuration with "--", if its argument parser supports that).
 *
 * Note that when run via `arc diff`, the list of files to be linted includes
 * deleted files and files that were moved away by the change. The linter should
 * not assume any of the paths it is given exists, and it is not an error for the
 * linter to be invoked with paths which are no longer there. (Every affected
 * path is subject to lint because some linters may raise errors in other files
 * when a file is removed, or raise an error about its removal.)
 *
 * The script should emit a JSON array of lint violations to stdout. A lint
 * violation may have the following attributes,
 *
 *   - `file` (required) The name of the file to raise the lint message in.
 *   - `message` (required) Text describing the lint message. For example,
 *     "This is a syntax error.".
 *   - `name` (optional) Text summarizing the lint message. For example,
 *     "Syntax Error".
 *   - `severity` (optional) The word "error", "warning", "autofix", "advice",
 *     or "disabled", in any combination of upper and lower case. Instead, you
 *   - `line` (optional) The line number of the message.
 *   - `char` (optional) The character offset of the message.
 *   - `offset` (optional) The byte offset of the message. If provided, this
 *     supersedes `line` and `char`.
 *   - `original` (optional) The text the message affects.
 *   - `replacement` (optional) The text that the range captured by `original`
 *     should be automatically replaced by to resolve the message.
 *   - `code` (optional) A short error type identifier which can be used
 *     elsewhere to configure handling of specific types of messages. For
 *     example, "EXAMPLE1", "EXAMPLE2", etc., where each code identifies a
 *     class of message like "syntax error", "missing whitespace", etc. This
 *     allows configuration to later change the severity of all whitespace
 *     messages, for example.
 *   - `throw` (optional) If set with a string error message `arc` will throw
 *     the given message. You can use this to fail abruptly if you
 *     encounter unexpected output. All processing will abort.
 *
 * For example, the following would encode a warning and an error,
 *
 *   [
 *     {
 *       'message': 'Too many goats!',
 *       'line': 13,
 *       'severity': 'error',
 *       'file': 'foo/bar/blah.bleh'
 *     },
 *     {
 *       'message': 'Not enough boats.',
 *       'line': 22,
 *       'severity: 'warning',
 *       'file': 'path/to/a/file.ext'
 *     }
 *   ]
 *
 * @task  lint        Linting
 * @task  linterinfo  Linter Information
 * @task  parse       Parsing Output
 * @task  config      Validating Configuration
 */
final class ArcanistExternalJsonLinter extends ArcanistLinter {

  private $script = null;
  private $output = "";

  public function getInfoName() {
    return pht('External JSON');
  }

  public function getInfoDescription() {
    return pht(
      'Run an external script, then parse its output as a JSON document'.
      'describing the lint violations. This is a generic binding that can '.
      'be used to run custom lint scripts.');
  }

  protected function shouldLintBinaryFiles() {
    return true;
  }

  protected function shouldLintDeletedFiles() {
    return true;
  }

  protected function shouldLintDirectories() {
    return true;
  }

  protected function shouldLintSymbolicLinks() {
    return true;
  }


/* -(  Linting  )------------------------------------------------------------ */


  /**
   * Pass the list of files to the script, and get the output.
   *
   * @task lint
   */
  public function willLintPaths(array $paths) {
    $root = $this->getProjectRoot();

    $future = new ExecFuture('%C %Ls', $this->script, $paths);
    list($this->output) = $future->resolvex();
  }

  /**
   * Parse the output produced by the script.
   *
   * @task lint
   */
  public function didLintPaths(array $paths) {
    if (!strlen($this->output)) {
      // No output, but it exited 0, so just move on.
      return;
    }

    $messages = phutil_json_decode($this->output, true);

    foreach ($messages as $message) {
      if (!empty($message['throw'])) {
          $throw = $message['throw'];
          throw new ArcanistUsageException(
            pht(
              "%s: linter threw an exception: '%s'\n",
              __CLASS__,
              $throw));
      }

      $line = idx($message, 'line');
      if ($line) {
        $line = (int)$line;
      } else {
        $line = null;
      }
      $char = idx($message, 'char');
      if ($char) {
          $char = (int)$char;
      } else {
          $char = null;
      }

      $path = idx($message, 'file');
      $code = idx($message, 'code', $this->getLinterName());
      $severity = $this->getMessageSeverity($message);
      $name = idx($message, 'name', 'Lint');
      $description = idx($message, 'message', pht('Undefined Lint Message'));

      $lint = id(new ArcanistLintMessage())
        ->setPath($path)
        ->setLine($line)
        ->setChar($char)
        ->setCode($code)
        ->setSeverity($severity)
        ->setName($name)
        ->setDescription($description);

      $original = idx($message, 'original');
      if ($original !== null) {
        $lint->setOriginalText($original);
      }

      $replacement = idx($message, 'replacement');
      if ($replacement !== null) {
        $lint->setReplacementText($replacement);
      }

      $this->addLintMessage($lint);
    }
  }


/* -(  Linter Information  )------------------------------------------------- */

  /**
   * Return the short name of the linter.
   *
   * @return string Short linter identifier.
   *
   * @task linterinfo
   */
  public function getLinterName() {
    return 'ExtJson';
  }

  public function getLinterConfigurationName() {
    return 'external-json';
  }

  public function getLinterConfigurationOptions() {
    // These fields are optional only to avoid breaking things.
    $options = array(
      'external-json.script' => array(
        'type' => 'string',
        'help' => pht('Script to execute.'),
      ),
    );

    return $options + parent::getLinterConfigurationOptions();
  }

  public function setLinterConfigurationValue($key, $value) {
    switch ($key) {
      case 'external-json.script':
        $this->script = $value;
        return;
    }

    return parent::setLinterConfigurationValue($key, $value);
  }

/* -(  Parsing Output  )----------------------------------------------------- */

  /**
   * Map the regex matching groups to a message severity. We look for either
   * a nonempty severity name group like 'error', or a group called 'severity'
   * with a valid name.
   *
   * @param dict message object
   * @return const  @{class:ArcanistLintSeverity} constant.
   *
   * @task parse
   */
  private function getMessageSeverity(array $message) {
    $map = array(
      'error'    => ArcanistLintSeverity::SEVERITY_ERROR,
      'warning'  => ArcanistLintSeverity::SEVERITY_WARNING,
      'autofix'  => ArcanistLintSeverity::SEVERITY_AUTOFIX,
      'advice'   => ArcanistLintSeverity::SEVERITY_ADVICE,
      'disabled' => ArcanistLintSeverity::SEVERITY_DISABLED,
    );

    if (idx($message, 'severity')) {
      $severity_name = phutil_utf8_strtolower(idx($message, 'severity'));
      if (!idx($map, $severity_name)) {
        throw new ArcanistUsageException(
          pht('%s: Unknown severity %s', __CLASS__, $severity_name));
      } else {
        return $map[$severity_name];
      }
    } else {
      return ArcanistLintSeverity::SEVERITY_ERROR;
    }
  }

}
