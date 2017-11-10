# sclinter
[![Build Status](https://img.shields.io/travis/scaledata/sclinter.svg)](https://travis-ci.org/scaledata/sclinter)
[![Test Coverage](https://img.shields.io/codecov/c/github/scaledata/sclinter.svg)](https://codecov.io/gh/scaledata/sclinter)

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
