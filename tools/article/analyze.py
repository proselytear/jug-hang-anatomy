import collections,json,pathlib,re,statistics,sys
root=pathlib.Path(__file__).resolve().parents[2]
for name in sys.argv[1:]:
 run=root/'evidence'/name
 if not(run/'summary.json').exists():continue
 summary=json.loads((run/'summary.json').read_text());rows=[json.loads(x)for x in(run/'requests.jsonl').read_text().splitlines()];rows=[r for r in rows if r['phase']=='measurement']
 def q(vals,p):
  vals=sorted(vals);n=(len(vals)-1)*p;l=int(n);h=min(l+1,len(vals)-1);return vals[l]+(vals[h]-vals[l])*(n-l)if vals else None
 summary['heavy_by_outcome']={}
 for key in sorted({r['outcome']or'status-'+str(r['status'])for r in rows if r['kind']=='heavy'}):
  sub=[r for r in rows if r['kind']=='heavy'and(r['outcome']or'status-'+str(r['status']))==key];summary['heavy_by_outcome'][key]={'count':len(sub),'p95_ms':q([r['latency_ms']for r in sub],.95)}
 series=collections.defaultdict(list);snapshots=[]
 for ev in map(json.loads,(run/'scrape-events.jsonl').read_text().splitlines()):
  if ev['phase']!='measurement':continue
  file=run/'scrape'/f"{ev['index']:04d}.txt"
  if not file.exists():continue
  current={}
  for line in file.read_text().splitlines():
   if line.startswith('#'):continue
   m=re.match(r'([a-zA-Z_:][a-zA-Z0-9_:]*)(\{[^}]*\})?\s+([-+.eE\d]+)',line)
   if not m:continue
   metric,labels,value=m.group(1),m.group(2)or'',float(m.group(3));key=metric+labels
   if metric in ['process_cpu_usage','system_cpu_usage','system_cpu_count','tomcat_threads_busy_threads','tomcat_threads_config_max_threads','hikaricp_connections_active','hikaricp_connections_pending','hikaricp_connections_max','resilience4j_circuitbreaker_failure_rate','resilience4j_circuitbreaker_slow_call_rate','hang_partner_inflight']:
    series[key].append(value)
   if metric=='resilience4j_circuitbreaker_state'and value==1:series['state='+re.search(r'state="([^"]+)"',labels).group(1)].append(1)
   if metric.startswith(('resilience4j_circuitbreaker_','hang_partner_rejections','hang_rate_limiter','hang_db_bulkhead')):current[key]=value
  snapshots.append({'at':ev['start_utc'],'metrics':current})
 summary['resource_series']={k:{'samples':len(v),'median':statistics.median(v),'min':min(v),'max':max(v)}for k,v in series.items()}
 summary['scrape_statuses']=dict(collections.Counter(str(ev['status'])for ev in map(json.loads,(run/'scrape-events.jsonl').read_text().splitlines())if ev['phase']=='measurement'))
 if snapshots:
  summary['counter_first_last_delta']={k:{'first':v,'last':snapshots[-1]['metrics'].get(k),'delta':snapshots[-1]['metrics'].get(k,0)-v}for k,v in snapshots[0]['metrics'].items()if ('_count{'in k or'_total{'in k)}
  (run/'cb-timeline.json').write_text(json.dumps(snapshots,indent=2))
 (run/'analysis.json').write_text(json.dumps(summary,indent=2));print(name,json.dumps(summary,ensure_ascii=False))
