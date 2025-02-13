shopt -s histappend
bind "set completion-ignore-case off"

alias devon="source ~/.devon/devon"
devon
source ~/.devon/autocomplete

alias ide="source ~/projects/_ide/bin/ide"
ide
ide init

source "$IDE_ROOT/_ide/functions"
source "$IDE_ROOT/_ide/installation/functions"
