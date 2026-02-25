curl "http://localhost:8888/coincoin/sync" \
      -d FORMAT="mivot" \
      -d REQUEST="doQuery" \
      -d LANG="ADQL" \
      -d QUERY='SELECT TOP 1 * FROM basic'
     