#already exists
autoload -U +X bashcompinit && bashcompinit
alias devon="source ~/.devon/devon"
devon
source ~/.devon/autocomplete
source "$IDE_ROOT/_ide/functions"
ide
ide init
source "$IDE_ROOT/_ide/installation/functions"
