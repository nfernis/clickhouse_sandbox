INSERT INTO medical_visits SELECT
  toDate('2026-05-18') + (rand() % 7),
  toUInt64(rand64() % 200000 + 1),
  ['J00','J01','A00','T50.9','I21','I63','Z00','R50','S06','O60'][rand() % 10 + 1],
  toUInt8(rand() % 100),
  ['M','F'][rand() % 2 + 1],
  [77,45,66,23,12][rand() % 5 + 1],
  ['Moscow','Tver','Ekb','Novosib','Kazan'][rand() % 5 + 1]
FROM numbers(500000)

/*Get-Content .\insert.sql | docker exec -i chart-clickhouse clickhouse-client --user=default --password=test123123*/