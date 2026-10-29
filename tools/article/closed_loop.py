#!/usr/bin/env python3
"""Closed-loop article measurements: save every response and raw metric scrape."""
import argparse, concurrent.futures,datetime,json,pathlib,statistics,threading,time,urllib.error,urllib.request
p=argparse.ArgumentParser();p.add_argument('out');p.add_argument('--base',default='http://127.0.0.1:28080');p.add_argument('--warmup',type=float,default=10);p.add_argument('--duration',type=float,default=60);p.add_argument('--heavy',type=int,default=12);p.add_argument('--admin',type=int,default=1);p.add_argument('--health',type=int,default=1);p.add_argument('--pace',type=float,default=1);p.add_argument('--probes',action='store_true');a=p.parse_args()
out=pathlib.Path(a.out)
if (out/'requests.jsonl').exists():p.error('output already contains requests.jsonl; use a new run directory')
(out/'scrape').mkdir(parents=True,exist_ok=True); lock=threading.Lock();events=[]
now=lambda:datetime.datetime.now(datetime.timezone.utc).isoformat(timespec='milliseconds').replace('+00:00','Z')
start=time.monotonic();measure=start+a.warmup;stop=measure+a.duration
f=(out/'requests.jsonl').open('w');s=(out/'scrape-events.jsonl').open('w')
def worker(kind,path,num,pace,background=False):
 while time.monotonic()<stop:
  tick=time.monotonic();stamp=time.time();code=None;data=b'';error=None
  try:
   with urllib.request.urlopen(a.base+path,timeout=45) as r:code=r.status;data=r.read()
  except urllib.error.HTTPError as e:code=e.code;data=e.read()
  except Exception as e:error=type(e).__name__+': '+str(e)
  elapsed=time.monotonic()-tick
  try:body=json.loads(data)
  except (ValueError,UnicodeDecodeError):body={}
  if not isinstance(body,dict):body={}
  partner=body.get('partner')or{}
  row={'start_epoch':stamp,'start_utc':datetime.datetime.fromtimestamp(stamp,datetime.timezone.utc).isoformat(timespec='milliseconds'),'end_utc':now(),'endpoint':path,'kind':kind,'worker':num,'background':background,'phase':'warmup' if tick<measure else 'measurement','status':code,'transport_error':error,'latency_ms':elapsed*1000,'outcome':body.get('outcome'),'partner_source':partner.get('source')if isinstance(partner,dict)else None,'body':data.decode('utf-8',errors='replace')}
  with lock:f.write(json.dumps(row,ensure_ascii=False)+'\n');f.flush();events.append(row)
  time.sleep(max(0,min(pace-elapsed,stop-time.monotonic())))
def scrape():
 i=0
 while time.monotonic()<stop:
  tick=time.monotonic();stamp=now();error=None;code=None
  try:
   with urllib.request.urlopen(a.base+'/actuator/prometheus',timeout=4)as r:code=r.status;data=r.read();(out/'scrape'/f'{i:04d}.txt').write_bytes(data)
  except Exception as e:error=type(e).__name__+': '+str(e)
  s.write(json.dumps({'index':i,'start_utc':stamp,'end_utc':now(),'phase':'warmup'if tick<measure else'measurement','status':code,'error':error})+'\n');s.flush();i+=1
  time.sleep(max(0,min(1-(time.monotonic()-tick),stop-time.monotonic())))
(out/'window.json').write_text(json.dumps({'start_utc':now(),'start_epoch':time.time(),'warmup_seconds':a.warmup,'measurement_seconds':a.duration,'pace_seconds':a.pace,'heavy':a.heavy,'admin':a.admin,'health':a.health,'client_timeout_seconds':45,'selection':'request start timestamp; in-flight requests drained after launch window','readiness_liveness_probes_every_seconds':5 if a.probes else None},indent=2))
with concurrent.futures.ThreadPoolExecutor(max_workers=a.heavy+a.admin+a.health+3)as pool:
 tasks=[pool.submit(scrape)]
 for kind,path,n in [('heavy','/api/v1/dashboard/00000000-0000-0000-0000-000000000001',a.heavy),('admin','/api/v1/admin/status',a.admin),('health','/actuator/health',a.health)]:
  tasks += [pool.submit(worker,kind,path,i,a.pace)for i in range(n)]
 if a.probes:
  for kind in ['readiness','liveness']:tasks.append(pool.submit(worker,kind,'/actuator/health/'+kind,0,5,True))
 for t in tasks:t.result()
f.close();s.close()
def quant(vals,q):
 vals=sorted(vals);idx=(len(vals)-1)*q;lo=int(idx);hi=min(lo+1,len(vals)-1);return vals[lo]+(vals[hi]-vals[lo])*(idx-lo)if vals else None
summary={'end_utc':now(),'total_elapsed_seconds':time.monotonic()-start,'groups':{}}
for kind in ['heavy','admin','health','readiness','liveness']:
 rows=[r for r in events if r['phase']=='measurement'and r['kind']==kind]
 if not rows:continue
 from collections import Counter
 summary['groups'][kind]={'count':len(rows),'statuses':dict(Counter(str(r['status'])for r in rows)),'outcomes':dict(Counter(str(r['outcome'])for r in rows)),'sources':dict(Counter(str(r['partner_source'])for r in rows)),'p95_ms':quant([r['latency_ms']for r in rows],.95),'p99_ms':quant([r['latency_ms']for r in rows],.99),'transport_errors':sum(bool(r['transport_error'])for r in rows)}
(out/'summary.json').write_text(json.dumps(summary,indent=2));print(json.dumps(summary),flush=True)
