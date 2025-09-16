curl "http://localhost:8888/vollt-mivot-extension/sync" \
      -d FORMAT="mivot" \
      -d REQUEST="doQuery" \
      -d LANG="ADQL" \
      -d QUERY='SELECT TOP 1 * FROM hip.hipparcos'
     