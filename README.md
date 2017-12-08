# sclinter
[![Build Status](https://img.shields.io/travis/scaledata/sclinter.svg)](https://travis-ci.org/scaledata/sclinter)
[![Test Coverage](https://img.shields.io/codecov/c/github/scaledata/sclinter.svg)](https://codecov.io/gh/scaledata/sclinter)
[![Codacy Grade](https://img.shields.io/codacy/grade/b6b3db4f4c1242aea2f9961f781c4307.svg)](https://www.codacy.com/app/sujeet_2/sclinter)

`sclinter` is a [`scala.meta`](http://scalameta.org/) based linter which aims to 
eventually encode [Rubrik's scala formatting guide](https://goo.gl/AjwKBy)
completely.

`sclinter` provides a [jar](target/scala-2.12/scala-linter-assembly-0.1.jar)
that produces lint output in JSON format directly consumable by
[`external-json-linter`](https://github.com/ghc/arcanist-external-json-linter)
for arcanist.

## Installation
First add this repository as a submodule of the project:
```bash
# go to project root
cd $(git rev-parse --show-toplevel)

# Add submodule
git submodule add git@github.com:scaledata/sclinter.git .scala-linter
git submodule update --init --recursive
```

Load the linter in `.arcconfig`
```json
{
  "project_id": "my-awesome-project",
  "conduit_uri": "https://example.org",

  "load": [
    ".scala-linter/.arcanist-external-json-linter"
  ]
}
```

Add the linter to `.arclint`:
```json
{
  "linters": {
    "scala-linter": {
      "type": "external-json",
      "include": "(\\.scala$)",
      "external-json.script": "java -jar .scala-linter/target/scala-2.12/scala-linter-assembly-0.1.jar $1"
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
./setup-git.sh

# build
sbt assembly

# only test
sbt test
```
