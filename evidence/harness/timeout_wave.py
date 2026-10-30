import concurrent.futures,datetime,json,pathlib,subprocess,threading,time,urllib.error,urllib.request
root=pathlib.Path(__file__).parent;out=root/'evidence'/'timeout-tail-confirm';(out/'scrape').mkdir(parents=True,exist_ok=True)
base='http://127.0.0.1:28080';endpoint='/api/v1/dashboard/00000000-0000-0000-0000-000000000001';stamp=lambda:datetime.datetime.now(datetime.timezone.utc).isoformat(timespec='milliseconds').replace('+00:00','Z')
def get(path):
 try:
  with urllib.request.urlopen(base+path,timeout=45)as r:return r.status,r.read()
 except urllib.error.HTTPError as e:return e.code,e.read()
def metric(label):
 _,data=get('/actuator/prometheus');(out/'scrape'/f'{label}.txt').write_bytes(data)
def pg(label):
 sql="SELECT count(*) FROM partner_snapshot WHERE dashboard_id='00000000-0000-0000-0000-000000000001';"
 r=subprocess.run(['docker','exec','hang-v5-postgres','psql','-U','hanganatomy','-d','hanganatomy','-P','pager=off','-c',sql],text=True,capture_output=True);(out/f'snapshot-count-{label}.txt').write_text(stamp()+'\n'+r.stdout+r.stderr)
def worker(i):
 tick=time.monotonic();start_epoch=time.time();beg=stamp();status,data=get(endpoint);elapsed=time.monotonic()-tick;body=json.loads(data)
 return {'start_epoch':start_epoch,'start_utc':beg,'end_utc':stamp(),'endpoint':endpoint,'kind':'heavy','worker':i,'background':False,'phase':'measurement','status':status,'latency_ms':elapsed*1000,'outcome':body.get('outcome'),'partner_source':body.get('partner',{}).get('source'),'transport_error':None,'body':data.decode()}
metric('before');pg('before');events=[];start=time.monotonic()
def scrape():
 for i in range(9):
  target=start+i;time.sleep(max(0,target-time.monotonic()));beg=stamp();status,data=get('/actuator/prometheus');(out/'scrape'/f'{i:04d}.txt').write_bytes(data);events.append({'index':i,'start_utc':beg,'end_utc':stamp(),'phase':'measurement','status':status,'error':None})
with concurrent.futures.ThreadPoolExecutor(max_workers=9)as pool:
 poll=pool.submit(scrape);rows=list(pool.map(worker,range(8)));last=time.monotonic();last_stamp=max(r['end_utc']for r in rows);(out/'requests.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n'for r in rows));time.sleep(max(0,last+2-time.monotonic()));dump_start=stamp()
 listing=subprocess.check_output(['docker','exec','hang-v5-app','jcmd','-l'],text=True);pid=next(x.split()[0]for x in listing.splitlines()if'/app/app.jar'in x)
 dump=subprocess.check_output(['docker','exec','hang-v5-app','jcmd',pid,'Thread.print','-l'],text=True);(out/'threads-after-last-response-2s.txt').write_text('captured_start_utc='+dump_start+'\nlast_response_utc='+last_stamp+'\n'+dump)
 chunks=dump.split('\n\n');(out/'threads-partner-exec-selected.txt').write_text('captured_start_utc='+dump_start+'\nlast_response_utc='+last_stamp+'\n'+'\n\n'.join(c for c in chunks if'"partner-exec-'in c))
 poll.result()
metric('after');pg('after');(out/'scrape-events.jsonl').write_text(''.join(json.dumps(e)+'\n'for e in events));(out/'window.json').write_text(json.dumps({'pattern':'single simultaneous wave8requests','warmup_seconds':0,'start_utc':min(r['start_utc']for r in rows),'last_response_utc':last_stamp,'dump_start_utc':dump_start,'partner_delay_ms':5000,'observed_until_utc':stamp()},indent=2));print(json.dumps({'outcomes':[r['outcome']for r in rows],'latencies_ms':[r['latency_ms']for r in rows],'last_response':last_stamp,'dump_at':dump_start}),flush=True)
