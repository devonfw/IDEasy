#--- Body ---
echo "Running install npm integration test"
$IDE_INSTALLATION -d install npm || return 1
return 0

