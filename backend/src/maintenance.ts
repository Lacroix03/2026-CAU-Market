import {cleanup} from './uploads';
cleanup().then(()=>process.exit(0)).catch(()=>{process.stderr.write('Cleanup failed; pending records retained.\n');process.exit(1);});
