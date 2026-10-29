#!/usr/bin/env python3
"""Create an isolated Compose file without modifying a source checkout."""
import argparse
import json
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--source', type=Path, required=True)
parser.add_argument('--out', type=Path, required=True)
parser.add_argument('--project', default='hang-article')
parser.add_argument('--port-offset', type=int, default=10000)
parser.add_argument('--legacy-pool-name', action='store_true',
                    help='Adapt only external YAML for the historical measured commits')
parser.add_argument('--image', help='Reuse a previously built image instead of building')
parser.add_argument('--subnet', help='Optional unused Docker subnet, for example 10.253.29.0/24')
args = parser.parse_args()
source, out = args.source.resolve(), args.out.resolve()
out.mkdir(parents=True, exist_ok=True)
cfg = json.loads(subprocess.check_output(
    ['docker', 'compose', '-f', str(source / 'docker-compose.yml'),
     'config', '--format', 'json'], text=True, cwd=source))
cfg['name'] = args.project
ports = {
    'app': [(8080, 8080)], 'grafana': [(3000, 3000)],
    'wiremock': [(8089, 8080)], 'postgres': [(5432, 5432)],
    'loki': [(3100, 3100)], 'mimir': [(9009, 9009)],
    'tempo': [(3200, 3200)],
    'alloy': [(4317, 4317), (4318, 4318), (12346, 12345)],
}
for name, service in cfg['services'].items():
    service['container_name'] = args.project + '-' + name
    if name in ports:
        service['ports'] = [f'127.0.0.1:{host + args.port_offset}:{target}'
                            for host, target in ports[name]]
for kind in ('networks', 'volumes'):
    for name, item in cfg.get(kind, {}).items():
        item['name'] = args.project + '-' + name
if args.subnet:
    for network in cfg.get('networks', {}).values():
        network['ipam'] = {'config': [{'subnet': args.subnet}]}
app = cfg['services']['app']
if args.image:
    app.pop('build', None)
    app['image'] = args.image
else:
    app['image'] = args.project + '-app'
app['cpus'], app['mem_limit'] = 2, '1536m'
for name in ('postgres', 'wiremock'):
    cfg['services'][name]['cpus'] = 2
env = app['environment']
env.update({
    'SPRING_PROFILES_ACTIVE': 'docker',
    'JAVA_OPTS': '-XX:MaxRAMPercentage=70 -Dfile.encoding=UTF-8',
    'SERVER_TOMCAT_THREADS_MAX': '16',
    'SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE': '6',
    'SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT': '12000',
    'HANG_PARTNER_READ_TIMEOUT': '15s',
    'HANG_PARTNER_OVERALL_DEADLINE': '15s',
})
for key in ('HANG_PARTNER_TIMEOUT_ENABLED', 'HANG_PARTNER_BULKHEAD_ENABLED',
            'HANG_PARTNER_CIRCUIT_BREAKER_ENABLED', 'HANG_DB_BULKHEAD_ENABLED',
            'HANG_RATE_LIMITER_HEAVY_ENABLED'):
    env[key] = 'false'
if args.legacy_pool_name:
    settings = out / 'application'
    settings.mkdir(exist_ok=True)
    for file in (source / 'src/main/resources').glob('application*.yml'):
        content = file.read_text()
        if file.name == 'application.yml':
            if '      pool-name: hang-hikari\n' not in content:
                raise SystemExit('Historical pool-name line not found; omit --legacy-pool-name')
            content = content.replace('      pool-name: hang-hikari\n', '')
            content = content.replace('  datasource:\n',
                                      '  datasource:\n    name: hang-hikari\n', 1)
        (settings / file.name).write_text(content)
    app.setdefault('volumes', []).append({
        'type': 'bind', 'source': str(settings),
        'target': '/article-config', 'read_only': True,
    })
    env['SPRING_CONFIG_LOCATION'] = 'file:/article-config/'
path = out / 'compose.json'
path.write_text(json.dumps(cfg, indent=2) + '\n')
sha = subprocess.check_output(['git', '-C', str(source), 'rev-parse', 'HEAD'], text=True).strip()
(out / 'source.json').write_text(json.dumps({
    'commit': sha, 'legacy_pool_name': args.legacy_pool_name,
    'profiles': env['SPRING_PROFILES_ACTIVE'],
    'project': args.project, 'port_offset': args.port_offset,
}, indent=2) + '\n')
print(path)
