let currentUser=null,patientAppointments=[],staffAppointments=[],activeRole='patient',staffTab='All';const $=id=>document.getElementById(id);const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));async function api(url,opts={}){let r=await fetch(url,{credentials:'same-origin',headers:{'Content-Type':'application/json',...(opts.headers||{})},...opts});let d={};try{d=await r.json()}catch{}if(!r.ok)throw Error(d.error||'Something went wrong.');return d}function toast(msg,error=false){let t=$('toast');t.textContent=msg;t.className='toast show'+(error?' error':'');clearTimeout(window.toastTimer);window.toastTimer=setTimeout(()=>t.className='toast',3200)}function go(id){$('dashboard').classList.add('hidden');$('app').classList.remove('hidden');document.querySelectorAll('main section').forEach(s=>s.classList.remove('section-active'));let el=$(id);if(el)el.scrollIntoView({behavior:'smooth',block:'start'});document.querySelector('#publicNav').classList.remove('open')}function closeModal(){$('authModal').classList.add('hidden')}function showAuth(mode){activeRole='patient';$('authModal').classList.remove('hidden');renderAuth(mode)}function renderAuth(mode){let c=$('authContent');if(mode==='signup'){c.innerHTML=`<h2 class="auth-title">Create an account</h2><p class="auth-sub">Join QueueLess and make your clinic visits easier.</p><div class="role-switch"><button class="${activeRole==='patient'?'active':''}" onclick="setRole('patient','signup')">Patient</button><button class="${activeRole==='staff'?'active':''}" onclick="setRole('staff','signup')">Staff</button></div><form onsubmit="signup(event)"><div class="field"><label>FULL NAME</label><input name="fullName" placeholder="Enter your full name" required></div><div class="field"><label>USERNAME</label><input name="username" placeholder="Choose a username" required></div><div class="field"><label>EMAIL ADDRESS</label><input name="email" type="email" placeholder="name@example.com" required></div><div class="field"><label>MOBILE NUMBER</label><input name="mobile" placeholder="09XXXXXXXXX" required></div><div class="form-row"><div class="field"><label>CREATE PASSWORD</label><input name="password" type="password" minlength="6" required></div><div class="field"><label>CONFIRM PASSWORD</label><input name="confirmPassword" type="password" minlength="6" required></div></div><button class="btn btn-primary full">Create account</button><p class="terms">By creating an account, you agree to the clinic's terms and regulations. Your information will be used for clinic registration and appointment management.</p></form><p class="auth-foot">Already have an account? <a href="#" onclick="renderAuth('login');return false">Log in</a></p>`}else if(mode==='forgot'){c.innerHTML=`<h2 class="auth-title">Reset your password</h2><p class="auth-sub">Enter your registered email or mobile number to request a verification code.</p><form onsubmit="requestCode(event)"><div class="field"><label>EMAIL OR MOBILE NUMBER</label><input name="identity" required placeholder="Email or mobile number"></div><button class="btn btn-primary full">Get verification code</button></form><div id="resetStep"></div><p class="auth-foot"><a href="#" onclick="renderAuth('login');return false">Back to log in</a></p>`}else{c.innerHTML=`<h2 class="auth-title">Welcome back!</h2><p class="auth-sub">Log in to your QueueLess account.</p><div class="role-switch"><button class="${activeRole==='patient'?'active':''}" onclick="setRole('patient','login')">Patient</button><button class="${activeRole==='staff'?'active':''}" onclick="setRole('staff','login')">Staff</button></div><form onsubmit="login(event)"><div class="field"><label>EMAIL OR MOBILE NUMBER</label><input name="identity" placeholder="Enter email or mobile number" required></div><div class="field"><label>PASSWORD</label><input name="password" type="password" placeholder="Enter password" required></div><div style="text-align:right"><button type="button" class="link-button" onclick="renderAuth('forgot')">Forgot password?</button></div><button class="btn btn-primary full">Log in</button></form><p class="auth-foot">Don't have an account? <a href="#" onclick="renderAuth('signup');return false">Sign in</a></p><p class="terms">Demo staff: admin@clinic.local / admin123<br>Demo doctor: doctor@clinic.local / doctor123</p>`}}function setRole(r,m){activeRole=r;renderAuth(m)}async function signup(e){e.preventDefault();let f=Object.fromEntries(new FormData(e.target));f.role=activeRole;try{let d=await api('/api/auth/signup',{method:'POST',body:JSON.stringify(f)});currentUser=d.user;closeModal();afterLogin();toast('Your account has been created.')}catch(err){toast(err.message,true)}}async function login(e){e.preventDefault();let f=Object.fromEntries(new FormData(e.target));f.role=activeRole;try{let d=await api('/api/auth/login',{method:'POST',body:JSON.stringify(f)});currentUser=d.user;closeModal();afterLogin();toast('Welcome back, '+currentUser.fullName+'.')}catch(err){toast(err.message,true)}}async function requestCode(e){e.preventDefault();let f=Object.fromEntries(new FormData(e.target));try{let d=await api('/api/auth/forgot',{method:'POST',body:JSON.stringify(f)});$('resetStep').innerHTML=`<div class="notice">${esc(d.message)}${d.demoCode?'<br><b>Demo verification code: '+esc(d.demoCode)+'</b>':''}</div><form onsubmit="resetPassword(event,'${esc(f.identity)}')"><div class="field"><label>VERIFICATION CODE</label><input name="code" required></div><div class="field"><label>NEW PASSWORD</label><input name="password" type="password" minlength="6" required></div><button class="btn btn-primary full">Reset password</button></form>`}catch(err){toast(err.message,true)}}async function resetPassword(e,id){e.preventDefault();let f=Object.fromEntries(new FormData(e.target));f.identity=id;try{await api('/api/auth/reset',{method:'POST',body:JSON.stringify(f)});toast('Password updated. You can now log in.');renderAuth('login')}catch(err){toast(err.message,true)}}async function afterLogin(){updateNav();$('app').classList.add('hidden');$('dashboard').classList.remove('hidden');$('welcomeName').textContent=currentUser.fullName; $('dashName').textContent=currentUser.fullName;$('avatar').textContent=(currentUser.fullName||'U').charAt(0).toUpperCase();$('dashRole').textContent=currentUser.role==='staff'?'Clinic staff':'Patient';$('dashDate').textContent=new Date().toLocaleDateString(undefined,{weekday:'long',month:'long',day:'numeric',year:'numeric'});$('sidePatient').classList.toggle('hidden',currentUser.role!=='patient');$('sideStaff').classList.toggle('hidden',currentUser.role!=='staff');if(currentUser.role==='staff')staffPage('dashboard');else{let p=await api('/api/patient/profile');if(!p.registered)patientPage('register');else patientPage('appointment')}}function updateNav(){let logged=!!currentUser;$('publicNav').classList.remove('hidden');document.querySelectorAll('#publicNav .nav-login,#publicNav .nav-cta').forEach(el=>el.classList.toggle('hidden',logged));$('userNav').classList.toggle('hidden',!logged);if(logged)$('welcomeName').textContent='Hi, '+currentUser.fullName.split(' ')[0]}function publicFromDashboard(id){$('dashboard').classList.add('hidden');$('app').classList.remove('hidden');go(id)}async function logout(){try{await api('/api/auth/logout',{method:'POST',body:'{}'})}catch{}currentUser=null;updateNav();$('dashboard').classList.add('hidden');$('app').classList.remove('hidden');go('home');toast('You have been logged out.')}function openDashboard(){if(!currentUser){showAuth('login');return}afterLogin()}function patientPage(page){$('dashTitle').textContent=({register:'Patient Registration',appointment:'Appointment',queue:'My Queue Status',profile:'My Profile'})[page];document.querySelectorAll('#sidePatient button').forEach(b=>b.classList.toggle('active',b.dataset.page===page));if(page==='register')renderPatientRegister();if(page==='appointment')renderPatientAppointment();if(page==='queue')renderPatientQueue();if(page==='profile')renderPatientProfile()}async function renderPatientRegister(){let p=await api('/api/patient/profile');$('dashContent').innerHTML=`<div class="page-heading"><div><h3>Patient Registration</h3><p>Complete your patient details before booking a clinic visit.</p></div></div><div class="content-card" style="max-width:760px"><form onsubmit="savePatientProfile(event)"><div class="form-row"><div class="field"><label>FULL NAME</label><input value="${esc(currentUser.fullName)}" disabled></div><div class="field"><label>AGE</label><input type="number" min="0" max="120" name="age" value="${esc(p.profile.age||'')}" required></div></div><div class="form-row"><div class="field"><label>MOBILE NUMBER</label><input value="${esc(currentUser.mobile)}" disabled></div><div class="field"><label>SEX</label><select name="sex" required><option value="">Select sex</option><option ${p.profile.sex==='Female'?'selected':''}>Female</option><option ${p.profile.sex==='Male'?'selected':''}>Male</option><option ${p.profile.sex==='Prefer not to say'?'selected':''}>Prefer not to say</option></select></div></div><div class="field"><label>PATIENT CATEGORY</label><select name="category" required><option value="">Choose patient category</option>${['Regular Patient','Pregnant','Senior Citizen','PWD','Legal Guardian'].map(x=>`<option ${p.profile.category===x?'selected':''}>${x}</option>`).join('')}</select></div><button class="btn btn-primary">Save registration details →</button></form></div>`}async function savePatientProfile(e){e.preventDefault();let f=Object.fromEntries(new FormData(e.target));try{await api('/api/patient/profile',{method:'POST',body:JSON.stringify(f)});toast('Patient registration saved.');patientPage('appointment')}catch(err){toast(err.message,true)}}async function renderPatientAppointment(){let d=await api('/api/appointments');patientAppointments=d.appointments;let pending=patientAppointments.find(a=>a.status==='Waiting'||a.status==='Checking');$('dashContent').innerHTML=`<div class="page-heading"><div><h3>Book an Appointment</h3><p>Select a service and an available clinic schedule.</p></div></div>${pending?`<div class="notice">You already have an active appointment (Q${pending.queue}). You can view it under My Queue. Cancel it before booking another appointment.</div>`:''}<div class="overview-grid"><div class="content-card"><form onsubmit="bookAppointment(event)"><div class="field"><label>SELECT MEDICAL SERVICE</label><select name="service" required><option value="">Choose a service</option>${['General Consultation','Dental','Maternal','Pediatrics','Laboratory'].map(x=>`<option>${x}</option>`).join('')}</select></div><div class="field"><label>APPOINTMENT DATE</label><input type="date" name="date" min="${new Date().toISOString().slice(0,10)}" required onchange="updateTimeOptions(this.value)"></div><div class="field"><label>PREFERRED TIME</label><select name="time" id="appointmentTime" required><option value="">Choose date first</option></select></div><div class="notice">Clinic hours: Monday–Friday, 8:00 AM–5:00 PM. Lunch break: 11:00 AM–1:00 PM. Appointment slots are one hour and subject to availability.</div><button class="btn btn-primary" ${pending?'disabled':''}>Register Queue →</button></form></div><div class="content-card"><h3>Appointment Tips</h3><div class="tip-box">• Arrive at least 1 hour before your appointment.<br>• Bring a valid ID and any relevant medical records.<br>• Check My Queue for your latest queue status.<br>• Please observe the clinic schedule.<br>• Your queue number will be assigned after booking.</div></div></div><div class="content-card" style="margin-top:17px"><h3>Recent Appointment Details</h3>${pending?`<p style="font-size:12px">${esc(pending.service)} · ${esc(pending.date)} · ${esc(pending.time)} · Queue Q${pending.queue}</p>`:'<p class="empty">No active appointment yet.</p>'}</div>`}function updateTimeOptions(d){let sel=$('appointmentTime');if(!sel)return;let day=new Date(d+'T12:00:00').getDay();if(day===0||day===6){sel.innerHTML='<option value="">Clinic is closed on weekends</option>';return}let slots=['08:00','09:00','10:00','13:00','14:00','15:00','16:00'];sel.innerHTML='<option value="">Choose time</option>'+slots.map(t=>`<option value="${t}">${new Date('2000-01-01T'+t).toLocaleTimeString([],{hour:'numeric',minute:'2-digit'})}</option>`).join('')}async function bookAppointment(e){e.preventDefault();let f=Object.fromEntries(new FormData(e.target));try{let d=await api('/api/appointments',{method:'POST',body:JSON.stringify(f)});toast('Appointment booked! Your queue number is Q'+d.appointment.queue);patientPage('queue')}catch(err){toast(err.message,true)}}async function refreshPatientQueue(button){if(button){button.disabled=true;button.textContent='↻ Refreshing...'}try{await renderPatientQueue()}catch(err){toast(err.message||'Unable to refresh queue status. Please try again.',true);if(button){button.disabled=false;button.textContent='↻ Refresh'}}}

function getAppointmentDateTime(dateValue, timeValue) {
    if (!dateValue || !timeValue) return null;

    const parts = String(dateValue).split('-').map(Number);
    if (parts.length !== 3 || parts.some(Number.isNaN)) return null;

    const match = String(timeValue).trim().match(
        /^(\d{1,2}):(\d{2})(?:\s*(AM|PM))?$/i
    );
    if (!match) return null;

    let hour = Number(match[1]);
    const minute = Number(match[2]);
    const period = match[3]?.toUpperCase();

    if (period === 'PM' && hour !== 12) hour += 12;
    if (period === 'AM' && hour === 12) hour = 0;

    return new Date(parts[0], parts[1] - 1, parts[2], hour, minute);
}

function formatQueueTime(date) {
    return date.toLocaleTimeString([], {
        hour: 'numeric',
        minute: '2-digit'
    });
}

function formatQueueDate(date) {
    return date.toLocaleDateString([], {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
    });
}

async function renderPatientQueue() {
    const data = await api('/api/appointments');
    patientAppointments = data.appointments || [];

    const appointment = patientAppointments
        .filter(item =>
            item.status === 'Waiting' || item.status === 'Checking'
        )
        .sort((a, b) => Number(a.queue) - Number(b.queue))[0];

    if (!appointment) {
        const last = patientAppointments
            .slice()
            .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))[0];

        $('dashContent').innerHTML = `
            <div class="page-heading">
                <div>
                    <h3>My Queue Status</h3>
                    <p>Track your appointment and queue progress.</p>
                </div>
                <button class="btn btn-outline btn-sm"
                    onclick="refreshPatientQueue(this)">↻ Refresh</button>
            </div>
            <div class="content-card empty">
                ${last
                    ? `Your latest appointment Q${last.queue} is ${esc(last.status)}.`
                    : 'You do not have an active queue yet.'}
                <br><br>
                <button class="btn btn-primary btn-sm"
                    onclick="patientPage('appointment')">Book an appointment</button>
            </div>
        `;
        return;
    }

    const ahead = patientAppointments.filter(item =>
        item.date === appointment.date &&
        Number(item.queue) < Number(appointment.queue) &&
        (item.status === 'Waiting' || item.status === 'Checking')
    ).length;

    const waitMinutes = ahead * 30;
    const position = ahead + 1;
    const suffix = position === 1 ? 'st' :
        position === 2 ? 'nd' :
        position === 3 ? 'rd' : 'th';

    const bookedDateTime = getAppointmentDateTime(
        appointment.date,
        appointment.time
    );

    if (!bookedDateTime || Number.isNaN(bookedDateTime.getTime())) {
        throw new Error('Unable to read your booked appointment date and time.');
    }

    // Patients must arrive one hour before their booked appointment.
    const arrivalDateTime = new Date(
        bookedDateTime.getTime() - 60 * 60 * 1000
    );

    // Estimated call time is based on the booked time, not the current time.
    const estimatedCallDateTime = new Date(
        bookedDateTime.getTime() + waitMinutes * 60 * 1000
    );

    $('dashContent').innerHTML = `
        <div class="page-heading">
            <div>
                <h3>My Queue Status</h3>
                <p>Check your current queue number and estimated waiting time.</p>
            </div>
            <button class="btn btn-outline btn-sm"
                onclick="refreshPatientQueue(this)">↻ Refresh</button>
        </div>

        <div class="queue-hero">
            <div>
                <small>YOUR QUEUE NUMBER</small>
                <strong>Q ${esc(appointment.queue)}</strong>
                <p>${esc(appointment.service)} · ${esc(appointment.date)} · ${esc(appointment.time)}</p>
            </div>
            <span class="pill ${esc(appointment.status)}">${esc(appointment.status)}</span>
        </div>

        <div class="queue-metrics">
            <div class="metric">
                <small>POSITION IN LINE</small>
                <b>${position}${suffix}</b>
                <span class="metric-note">Your place in the queue</span>
            </div>
            <div class="metric">
                <small>ESTIMATED WAIT</small>
                <b>${waitMinutes} min</b>
                <span class="metric-note">30 minutes per patient ahead</span>
            </div>
            <div class="metric arrival-metric">
                <small>ARRIVE AT THE CLINIC</small>
                <b>${formatQueueTime(arrivalDateTime)}</b>
                <span class="metric-note">${formatQueueDate(arrivalDateTime)} · 1 hour early</span>
            </div>
            <div class="metric call-metric">
                <small>ESTIMATED CALL TIME</small>
                <b>${formatQueueTime(estimatedCallDateTime)}</b>
                <span class="metric-note">${formatQueueDate(estimatedCallDateTime)}</span>
            </div>
        </div>

        <div class="tip-box queue-reminder">
            <strong>Important: Please arrive 1 hour before your appointment.</strong>
            <p>Your booked appointment is ${formatQueueTime(bookedDateTime)} on ${formatQueueDate(bookedDateTime)}.</p>
            <p>Please arrive at ${formatQueueTime(arrivalDateTime)} so the clinic can prepare you for your consultation.</p>
            <p>Your estimated call time is based on your booked appointment time plus 30 minutes for every patient ahead of you. It may change as the queue progresses.</p>
        </div>

        <div class="queue-actions">
            <button class="btn btn-sm btn-cancel-appointment"
                onclick="cancelMyAppointment('${esc(appointment.id)}')">
                Cancel appointment
            </button>
        </div>
    `;
}
async function cancelMyAppointment(id){if(!confirm('Cancel this appointment?'))return;try{await api('/api/appointments/cancel',{method:'POST',body:JSON.stringify({id})});toast('Appointment cancelled.');renderPatientQueue()}catch(err){toast(err.message,true)}}async function renderPatientProfile(){let [p,a]=await Promise.all([api('/api/patient/profile'),api('/api/appointments')]);let history=a.appointments.slice().sort((x,y)=>new Date(y.createdAt)-new Date(x.createdAt));$('dashContent').innerHTML=`<div class="page-heading"><div><h3>My Profile</h3><p>View your personal information and appointment history.</p></div></div><div class="content-card"><h3>Personal Information</h3><div class="profile-grid"><div class="profile-item"><small>FULL NAME</small><b>${esc(currentUser.fullName)}</b></div><div class="profile-item"><small>MOBILE NUMBER</small><b>${esc(currentUser.mobile)}</b></div><div class="profile-item"><small>EMAIL ADDRESS</small><b>${esc(currentUser.email)}</b></div><div class="profile-item"><small>PATIENT CATEGORY</small><b>${esc(p.profile.category||'Not registered')}</b></div><div class="profile-item"><small>AGE</small><b>${esc(p.profile.age||'—')}</b></div><div class="profile-item"><small>SEX</small><b>${esc(p.profile.sex||'—')}</b></div></div></div><div class="page-heading" style="margin-top:24px"><div><h3>Appointment History</h3><p>Your previous and current clinic visits.</p></div></div><div class="table-wrap"><table><thead><tr><th>DATE</th><th>TIME</th><th>SERVICE</th><th>QUEUE NO.</th><th>STATUS</th></tr></thead><tbody>${history.map(a=>`<tr><td>${esc(a.date)}</td><td>${esc(a.time)}</td><td>${esc(a.service)}</td><td>Q ${a.queue}</td><td><span class="pill ${a.status}">${esc(a.status)}</span></td></tr>`).join('')||'<tr><td colspan="5" class="empty">No appointment history.</td></tr>'}</tbody></table></div>`}
async function staffPage(page){$('dashTitle').textContent=({dashboard:'Dashboard',queue:'Queue Management',appointments:'Appointments',patients:'Patients'})[page];document.querySelectorAll('#sideStaff button').forEach(b=>b.classList.toggle('active',b.dataset.page===page));if(page==='dashboard')await renderStaffDashboard();if(page==='queue')await renderStaffQueue();if(page==='appointments')await renderStaffAppointments();if(page==='patients')await renderStaffPatients()}async function renderStaffDashboard(){let d=await api('/api/staff/dashboard');$('dashContent').innerHTML=`<div class="page-heading"><div><h3>Good day, ${esc(currentUser.fullName.split(' ')[0])}.</h3><p>Here's what's happening at your clinic today.</p></div><button class="btn btn-outline btn-sm" onclick="renderStaffDashboard()">↻ Refresh</button></div><div class="stat-grid"><div class="stat-card"><span class="stat-icon">♙</span><small>Total Patients</small><strong>${d.totalPatients}</strong><span>Registered patients</span></div><div class="stat-card"><span class="stat-icon">▣</span><small>Today's Appointments</small><strong>${d.todaysAppointments}</strong><span>Scheduled for today</span></div><div class="stat-card"><span class="stat-icon">◷</span><small>In Queue</small><strong>${d.inQueue}</strong><span>Waiting and checking</span></div><div class="stat-card"><span class="stat-icon">✓</span><small>Completed Today</small><strong>${d.completedToday}</strong><span>Consultations finished</span></div></div><div class="overview-grid"><div class="panel"><h3>Today's Queue Overview</h3><div class="status-grid">${Object.entries(d.counts).map(([k,v])=>`<div class="status-tile"><small>${k}</small><b>${v}</b></div>`).join('')}</div></div><div class="panel"><h3>Recent Activity</h3>${d.activities.map(a=>`<div class="activity"><span>${esc(a.message)}</span><small>${esc(a.time)}</small></div>`).join('')||'<div class="empty">No recent activity.</div>'}</div></div>`}async function renderStaffQueue(){let d=await api('/api/staff/queue');staffAppointments=d.appointments;let items=staffAppointments.filter(a=>staffTab==='All'||a.status===staffTab);$('dashContent').innerHTML=`<div class="page-heading"><div><h3>Queue Management</h3><p>Monitor patient flow, update consultation status, and manage released slots.</p></div></div><div class="toolbar"><input id="queueSearch" class="search" placeholder="Search patient name or queue number" oninput="filterTable('queueTable',this.value)"><button class="btn btn-outline btn-sm" onclick="showWalkinModal()">＋ Add Patient</button><button class="btn btn-primary btn-sm" onclick="callNext()">Call Next →</button></div><div class="tabs">${['All','Waiting','Checking','Completed','Cancelled','No-Show','Re-Assigned'].map(t=>`<button class="tab ${staffTab===t?'active':''}" onclick="staffTab='${t}';renderStaffQueue()">${t} (${t==='All'?staffAppointments.length:staffAppointments.filter(a=>a.status===t).length})</button>`).join('')}</div><div class="table-wrap"><table id="queueTable"><thead><tr><th>QUEUE</th><th>PATIENT NAME</th><th>CATEGORY</th><th>SERVICE</th><th>STATUS</th><th>ACTION</th><th>DETAILS</th></tr></thead><tbody>${items.sort((a,b)=>a.queue-b.queue).map(a=>`<tr><td>Q ${a.queue}</td><td><b>${esc(a.patientName)}</b>${a.walkIn?' <small>(Walk-in)</small>':''}</td><td>${esc(a.category||'—')}</td><td>${esc(a.service)}</td><td><span class="pill ${a.status}">${esc(a.status)}</span></td><td>${queueActions(a)}</td><td><button class="table-action" onclick="viewAppointment('${a.id}')">View</button></td></tr>`).join('')||'<tr><td colspan="7" class="empty">No queue records found.</td></tr>'}</tbody></table></div><p class="terms">Re-Assign is available only after an appointment is Cancelled or marked No-Show. Checking is used when a doctor begins consultation.</p>`}function queueActions(a){if(a.status==='Waiting')return `<button class="table-action" onclick="staffAction('${a.id}','checking')">Checking</button><button class="table-action warn" onclick="staffAction('${a.id}','no-show')">No-Show</button>`;if(a.status==='Checking')return `<button class="table-action" onclick="staffAction('${a.id}','complete')">Complete</button>`;if(a.status==='Cancelled'||a.status==='No-Show')return `<button class="table-action warn" onclick="reassign('${a.id}')">Re-Assign</button>`;return '—'}async function staffAction(id,action){let msg=action==='checking'?'Start checking this patient?':action==='complete'?'Mark this consultation as completed?':'Mark this patient as No-Show?';if(!confirm(msg))return;try{await api('/api/staff/action',{method:'POST',body:JSON.stringify({id,action})});toast('Queue status updated.');renderStaffQueue()}catch(err){toast(err.message,true)}}function filterTable(id,q){let rows=document.querySelectorAll('#'+id+' tbody tr');rows.forEach(r=>r.style.display=r.innerText.toLowerCase().includes(q.toLowerCase())?'':'none')}function callNext(){let a=staffAppointments.filter(x=>x.status==='Waiting').sort((x,y)=>x.queue-y.queue)[0];if(!a){toast('There are no waiting patients.');return}toast('Next patient: Q'+a.queue+' · '+a.patientName);staffAction(a.id,'checking')}function reassign(id){let a=staffAppointments.find(x=>x.id===id);if(!a||!['Cancelled','No-Show'].includes(a.status)){toast('Only cancelled or no-show appointments can be reassigned.',true);return}let name=prompt('Enter the walk-in patient full name:');if(!name)return;let category=prompt('Patient category (Regular Patient, Pregnant, Senior Citizen, PWD, Legal Guardian):','Regular Patient')||'Regular Patient';let age=prompt('Patient age (optional):','');let mobile=prompt('Mobile number (optional):','');api('/api/staff/action',{method:'POST',body:JSON.stringify({id,action:'reassign',patientName:name,category,age,mobile})}).then(d=>{toast('Released slot reassigned to walk-in patient Q'+d.newAppointment.queue);renderStaffQueue()}).catch(e=>toast(e.message,true))}function showWalkinModal(){let n=prompt('Walk-in patient full name:');if(!n)return;let category=prompt('Patient category:','Regular Patient')||'Regular Patient';let service=prompt('Service (General Consultation, Dental, Maternal, Pediatrics, Laboratory):','General Consultation')||'General Consultation';api('/api/staff/walkin',{method:'POST',body:JSON.stringify({fullName:n,category,service})}).then(d=>{toast('Walk-in patient added as Q'+d.appointment.queue);renderStaffQueue()}).catch(e=>toast(e.message,true))}function viewAppointment(id){let a=staffAppointments.find(x=>x.id===id);if(!a)return;alert(`PATIENT DETAILS\n\nName: ${a.patientName}\nMobile: ${a.mobile||'—'}\nEmail: ${a.email||'—'}\nSex: ${a.sex||'—'}\nAge: ${a.age||'—'}\nCategory: ${a.category||'—'}\n\nAPPOINTMENT\nService: ${a.service}\nDate: ${a.date}\nTime: ${a.time}\nQueue: Q${a.queue}\nStatus: ${a.status}`)}async function renderStaffAppointments(){let d=await api('/api/staff/queue');staffAppointments=d.appointments;let groups=['Upcoming','Completed','Cancelled','Re-Assigned','No-Show'];let upcoming=staffAppointments.filter(a=>a.status==='Waiting'||a.status==='Checking');$('dashContent').innerHTML=`<div class="page-heading"><div><h3>Appointments</h3><p>Manage scheduled appointments and appointment outcomes.</p></div></div><div class="stat-grid">${groups.map((g,i)=>`<div class="stat-card"><small>${g}</small><strong>${g==='Upcoming'?upcoming.length:staffAppointments.filter(a=>a.status===g).length}</strong><span>Appointment records</span></div>`).join('')}</div><div class="toolbar"><input class="search" placeholder="Search appointments" oninput="filterTable('appointmentTable',this.value)"></div><div class="table-wrap"><table id="appointmentTable"><thead><tr><th>DATE & TIME</th><th>PATIENT</th><th>SERVICE</th><th>DOCTOR</th><th>STATUS</th><th>VIEW</th></tr></thead><tbody>${staffAppointments.slice().sort((a,b)=>a.date.localeCompare(b.date)).map(a=>`<tr><td>${esc(a.date)}<br>${esc(a.time)}</td><td><b>${esc(a.patientName)}</b></td><td>${esc(a.service)}</td><td>${esc(a.doctor||'To be assigned')}</td><td><span class="pill ${a.status}">${esc(a.status)}</span></td><td><button class="table-action" onclick="viewAppointment('${a.id}')">View</button></td></tr>`).join('')||'<tr><td colspan="6" class="empty">No appointments yet.</td></tr>'}</tbody></table></div>`}async function renderStaffPatients(){let d=await api('/api/staff/patients');$('dashContent').innerHTML=`<div class="page-heading"><div><h3>Patients</h3><p>Search and review registered patient records.</p></div></div><div class="toolbar"><input class="search" placeholder="Search patient name or contact number" oninput="filterTable('patientsTable',this.value)"></div><div class="table-wrap"><table id="patientsTable"><thead><tr><th>PATIENT NAME</th><th>AGE</th><th>SEX</th><th>CONTACT NO.</th><th>CATEGORY</th><th>DETAILS</th></tr></thead><tbody>${d.patients.map(p=>`<tr><td><b>${esc(p.fullName)}</b></td><td>${esc(p.age||'—')}</td><td>${esc(p.sex||'—')}</td><td>${esc(p.mobile)}</td><td>${esc(p.category||'—')}</td><td><button class="table-action" onclick="viewPatient('${p.id}')">View</button></td></tr>`).join('')||'<tr><td colspan="6" class="empty">No registered patients yet.</td></tr>'}</tbody></table></div>`}async function viewPatient(id){try{let d=await api('/api/staff/patient/'+id);let p=d.patient;let hist=d.history.map(a=>`${a.date} | ${a.time} | ${a.service} | Q${a.queue} | ${a.status}`).join('\n')||'No appointment history';alert(`PATIENT INFORMATION\n\nFull Name: ${p.fullName}\nMobile: ${p.mobile}\nEmail: ${p.email}\nSex: ${p.sex||'—'}\nAge: ${p.age||'—'}\nCategory: ${p.category||'—'}\n\nAPPOINTMENT HISTORY\n${hist}`)}catch(e){toast(e.message,true)}}async function init(){try{let d=await api('/api/auth/me');if(d.user){currentUser=d.user;updateNav();$('app').classList.add('hidden');$('dashboard').classList.remove('hidden');$('welcomeName').textContent=currentUser.fullName;afterLogin()}else updateNav()}catch{updateNav()}}init();
