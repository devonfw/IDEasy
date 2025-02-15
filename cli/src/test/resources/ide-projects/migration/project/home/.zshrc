autoload -U +X bashcompinit && bashcompinit

alias ide="source ~/projects/_ide/bin/ide"
ide
ide init

source "$IDE_ROOT/_ide/functions"
source "$IDE_ROOT/_ide/installation/functions"
