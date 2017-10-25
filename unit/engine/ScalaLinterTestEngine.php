<?php

final class ScalaLinterTestEngine extends ArcanistUnitTestEngine {

  public function runAllTests() {
    return true;
  }

  public function shouldEchoTestResults() {
    return true;
  }

  public function supportsRunAllTests() {
    return true;
  }

  public function run() {
    $console = PhutilConsole::getConsole();
    $project_root = $this->getWorkingCopy()->getProjectRoot();
    $reports_dir = 'target/test-reports';
    $coverage_file = 'target/scala-2.12/coverage-report/cobertura.xml';
    $clear_reports_cmd = csprintf(
      'cd %s; rm -rf %s;',
      $project_root,
      $reports_dir
    );
    
    $test_cmd = $clear_reports_cmd.'sbt coverage test coverageReport;';
    $this->execCmd($test_cmd);

    $all_results = array();
    $output_dir = $project_root.'/'.$reports_dir;
    $handle = opendir($output_dir);

    $coverage_report = $this->readCoverageReport(
      $project_root.'/'.$coverage_file
    );

    while (false !== ($entry = readdir($handle))) {
      if (preg_match('@TEST@', $entry)) {
        $parser = new ArcanistXUnitTestResultParser();
        $results = $parser->parseTestResults(
          Filesystem::readFile($output_dir.'/'.$entry)
        );
        foreach ($results as $result) {
          $result->setCoverage($coverage_report);
        }
        $all_results = array_merge($all_results, $results);
      }
    }

    $this->execCmd($clear_reports_cmd);
    return $all_results;
  }

  private function execCmd($cmd) {
    $future = new ExecFuture('%C', $cmd);
    try {
      list($stdout, $stderr) = $future->resolvex();
    } catch (CommandException $e) {
      $console->writeErr($cmd.' failed: %s', $e->getStdout());
      exit(1);
    }
  }

  private function readCoverageReport($path) {
    $coverage_data = Filesystem::readFile($path);
    if (empty($coverage_data)) {
       return array();
    }

    $coverage_dom = new DOMDocument();
    $coverage_dom->loadXML($coverage_data);

    $paths = $this->getPaths();
    $reports = array();
    $classes = $coverage_dom->getElementsByTagName('class');

    foreach ($classes as $class) {
      // filename as mentioned in the report is relative to `src/main/scala/`,
      // but it needs to be relative to the project root.
      $relative_path = 'src/main/scala/'.$class->getAttribute('filename');

      $absolute_path = Filesystem::resolvePath($relative_path);

      if (!file_exists($absolute_path)) {
        continue;
      }

      // skip reporting coverage for files that aren't in the diff
      if (!in_array($relative_path, $paths)) {
        continue;
      }

      // get total line count in file
      $line_count = count(file($absolute_path));

      $coverage = '';
      $start_line = 1;
      $lines = $class->getElementsByTagName('line');
      for ($ii = 0; $ii < $lines->length; $ii++) {
        $line = $lines->item($ii);

        $next_line = (int)$line->getAttribute('number');
        for ($start_line; $start_line < $next_line; $start_line++) {
            $coverage .= 'N';
        }

        if ((int)$line->getAttribute('hits') == 0) {
            $coverage .= 'U';
        } else if ((int)$line->getAttribute('hits') > 0) {
            $coverage .= 'C';
        }

        $start_line++;
      }

      if ($start_line < $line_count) {
        foreach (range($start_line, $line_count) as $line_num) {
          $coverage .= 'N';
        }
      }

      $reports[$relative_path] = $coverage;
    }

    return $reports;
  }
}
