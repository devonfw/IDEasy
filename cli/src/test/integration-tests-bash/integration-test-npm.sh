#--- Body ---
echo "Running install npm integration test"
doIdeCreate
$IDE -d install npm || return 1
return 0

