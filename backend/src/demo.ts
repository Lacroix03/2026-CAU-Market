import {cleanup} from "./uploads";
cleanup().then(()=>{
 process.stdout.write("CAU Market local demo ready. Press Ctrl+C to export and stop.\n");
 setInterval(()=>undefined,60000);
}).catch(()=>{process.stderr.write("Startup cleanup failed; pending records retained.\n");process.exit(1);});
