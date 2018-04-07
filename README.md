# sclinter
[![Build Status](https://img.shields.io/travis/scaledata/sclinter.svg)](https://travis-ci.org/scaledata/sclinter)
[![GitHub release](https://img.shields.io/github/release/scaledata/sclinter.svg)](https://github.com/scaledata/sclinter/releases/latest)
[![Test Coverage](https://img.shields.io/codecov/c/github/scaledata/sclinter.svg)](https://codecov.io/gh/scaledata/sclinter)
[![Codacy Grade](https://img.shields.io/codacy/grade/b6b3db4f4c1242aea2f9961f781c4307.svg)](https://www.codacy.com/app/sujeet_2/sclinter)

`sclinter` is a [`scala.meta`](http://scalameta.org/) based linter which aims to 
eventually encode [Rubrik's scala formatting guide](https://goo.gl/AjwKBy)
completely.

`sclinter` provides both
[a `.jar` file, and a `.js` file](https://github.com/scaledata/sclinter/releases/latest)
that produces lint output in JSON format directly consumable by
[`external-json-linter`](lint/src/ArcanistExternalJsonLinter.php)
for arcanist.

## Installation

#### [Download](https://github.com/scaledata/sclinter/releases/latest)
and extract it in a folder, say `/opt/usr/lib/sclinter`

#### Load the linter in `.arcconfig`
```json
{
  "project_id": "my-awesome-project",
  "conduit_uri": "https://example.org",

  "load": [
    "/opt/usr/lib/sclinter/lint"
  ]
}
```

#### Add the linter to `.arclint` (invoked with JVM, runs faster)
```json
{
  "linters": {
    "scala-linter": {
      "type": "external-json",
      "include": "(\\.scala$)",
      "external-json.script": "java -jar /opt/usr/lib/sclinter/sclitner.jar $1"
    }
  }
}
```

#### Or, add the linter to `.arclint` (invoked with `node`, slower)
```json
{
  "linters": {
    "scala-linter": {
      "type": "external-json",
      "include": "(\\.scala$)",
      "external-json.script": "node /opt/usr/lib/sclinter/sclitner.js $1"
    }
  }
}
```

## Configuration

#### Disabling `sclinter` for a particular line
Add one of `sclinter:off`, `nolint`, `noqa`, `lint:off` as
a comment to the line, where you want to ignore lint errors.

###### Example:
```scala
// The type of the following function is a mouthful, and doesn't
// help in documentation even if written out explicitly.
def complexTypedFunction = { // nolint
  implementation
}

// You can add short explanations after the comment too
if(condition){ // lint:off I know I'm messing with whitespace, <explanation>.
  awesomeStuff()
}
```

## Development
Install `sbt` by following the
[installation guide](http://www.scala-sbt.org/release/docs/Setup.html)
```bash
git clone https://github.com/scaledata/sclinter.git
cd sclinter

# build
sbt sclinterJVM/assembly # builds the jar
sbt sclinterJS/fastOptJS # builds the JS file

# only test
sbt test
```
