#!/usr/bin/env bash
set -eu

mkdir -p "${CARGO_HOME}/bin"
mkdir -p "${RUSTUP_HOME}"

cat > "${CARGO_HOME}/bin/rustc" <<'EOF'
#!/usr/bin/env bash
echo rustc "$@"
EOF
chmod +x "${CARGO_HOME}/bin/rustc"

cat > "${CARGO_HOME}/bin/rustc.cmd" <<'EOF'
@echo off
echo rustc %*
EOF

