#!/bin/bash
mvn exec:exec -Dexec.executable=java -Dexec.args="-cp %classpath com.devonfw.tools.ide.cli.Ideasy $@"
