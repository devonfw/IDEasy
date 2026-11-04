echo "Running Pandoc installation and conversion integration test"

(
  ide --batch install pandoc || return 1
  pandoc_test_dir="$(mktemp -d ./pandoc-test.XXXXXX)" || return 1
  trap 'rm -rf -- "$pandoc_test_dir"' EXIT

  printf '# Pandoc smoke test\n' > "$pandoc_test_dir/input document.md" || return 1
  ide --batch pandoc --from=markdown --to=html "$pandoc_test_dir/input document.md" \
    --output "$pandoc_test_dir/output document.html" || return 1
  assertThat "$(cat "$pandoc_test_dir/output document.html")" contains 'Pandoc smoke test</h1>'
)
