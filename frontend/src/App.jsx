import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import CustomerView from './components/CustomerView';
import StaffView from './components/StaffView';
import AnalyticsView from './components/AnalyticsView';
import AiInspectorView from './components/AiInspectorView';
import {
  login,
  register,
  getMe,
  getStoredUser,
  removeToken,
  createBusiness,
  createService,
  openQueue,
  getQueue,
  getActiveQueues,
  getMLHealth
} from './services/api';

export default function App() {
  const [currentTab, setCurrentTab] = useState('customer');
  const [currentUser, setCurrentUser] = useState(null);
  const [currentQueue, setCurrentQueue] = useState(null);
  const [availableQueues, setAvailableQueues] = useState([]);
  const [mlHealth, setMlHealth] = useState(null);
  const [initializing, setInitializing] = useState(true);

  // Initialize clinic services and open queues
  const ensureDemoQueue = async () => {
    try {
      const ownerEmail = `owner_${Date.now()}@queueless.com`;
      await register({
        name: 'Dr. Sarah Lin (Chief of Staff)',
        email: ownerEmail,
        password: 'Password123!',
        role: 'BUSINESS_OWNER'
      });
      await login(ownerEmail, 'Password123!');

      const biz = await createBusiness({
        name: 'Apex Diagnostic Imaging Clinic',
        address: '100 Medical Center Blvd, Suite 400',
        phone: '555-432-1098',
        description: 'Advanced multi-specialty and diagnostic clinic'
      });

      const svc1 = await createService(biz.id, {
        name: 'General Consultation & Triage',
        description: 'Primary health consultation, vitals, and physician checkup',
        estimatedDurationMinutes: 15
      });

      const svc2 = await createService(biz.id, {
        name: 'MRI Scan & Diagnostic Review',
        description: 'Comprehensive 50-point diagnostic scan and radiology',
        estimatedDurationMinutes: 30
      });

      const svc3 = await createService(biz.id, {
        name: 'Blood Test & Routine Lab Work',
        description: 'Fast blood draw, metabolic panel, and lab sample intake',
        estimatedDurationMinutes: 10
      });

      const q1 = await openQueue(svc1.id, 2);
      const q2 = await openQueue(svc2.id, 2);
      const q3 = await openQueue(svc3.id, 1);

      const allQueues = [
        { ...q1, service: svc1, business: biz },
        { ...q2, service: svc2, business: biz },
        { ...q3, service: svc3, business: biz }
      ];

      setAvailableQueues(allQueues);
      setCurrentQueue(allQueues[0]);

      const custEmail = `customer_${Date.now()}@example.com`;
      await register({
        name: 'Alex Johnson',
        email: custEmail,
        password: 'Password123!',
        role: 'CUSTOMER'
      });
      const custAuth = await login(custEmail, 'Password123!');
      setCurrentUser(custAuth.user);

      return allQueues[0];
    } catch (e) {
      console.warn("Demo seed failed:", e);
    }
  };


  useEffect(() => {
    async function startup() {
      // Check Python ML health
      const health = await getMLHealth();
      setMlHealth(health);

      // Restore session or seed demo queue
      const stored = getStoredUser();
      if (stored) {
        try {
          const profile = await getMe();
          setCurrentUser(profile);
        } catch (e) {
          removeToken();
          setCurrentUser(null);
        }
      }

      // Check URL parameters for direct QR code joins: ?queue=1
      const params = new URLSearchParams(window.location.search);
      const urlQueueId = params.get('queue') || params.get('queueId');

      // Check for persistent active clinic queues from backend first
      let active = [];
      try {
        active = await getActiveQueues();
      } catch (err) {
        console.warn("Could not fetch active queues from backend", err);
      }

      if (active && active.length > 0) {
        setAvailableQueues(active);
        const matched = urlQueueId ? active.find(q => String(q.id) === String(urlQueueId)) : null;
        setCurrentQueue(matched || active[0]);
      } else {
        await ensureDemoQueue();
      }

      setInitializing(false);
    }

    startup();
  }, []);

  const handleQuickSwitchUser = async (role) => {
    try {
      if (role === 'BUSINESS_OWNER') {
        try {
          const auth = await login('owner@apexclinic.com', 'Password123!');
          setCurrentUser(auth.user);
          setCurrentTab('staff');
          return;
        } catch (e) {
          console.warn("Default owner login fallback to dynamic registration", e);
        }
      }

      const email = `demo_${role.toLowerCase()}_${Date.now()}@example.com`;
      await register({
        name: role === 'BUSINESS_OWNER' ? 'Dr. Sarah Lin (Staff / Owner)' : 'Alex Johnson (Patient)',
        email,
        password: 'Password123!',
        role
      });
      const auth = await login(email, 'Password123!');
      setCurrentUser(auth.user);
      if (role === 'BUSINESS_OWNER') {
        setCurrentTab('staff');
      } else {
        setCurrentTab('customer');
      }
    } catch (e) {
      alert(`Could not switch user: ${e.message}`);
    }
  };

  const handleLogout = () => {
    removeToken();
    setCurrentUser(null);
  };

  const refreshCurrentQueue = async () => {
    if (currentQueue?.id) {
      try {
        const q = await getQueue(currentQueue.id);
        setCurrentQueue(prev => ({ ...prev, ...q }));
      } catch (e) {
        console.warn("Failed to refresh queue", e);
      }
    }
  };

  if (initializing) {
    return (
      <div style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '16px'
      }}>
        <div className="pulse-indicator" style={{ width: '16px', height: '16px' }}></div>
        <div style={{ fontSize: '1.1rem', fontWeight: 600, color: 'var(--text-muted)' }}>
          Connecting to QueueLess AI & Python ML Engine...
        </div>
      </div>
    );
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Navbar
        currentTab={currentTab}
        setCurrentTab={setCurrentTab}
        currentUser={currentUser}
        onQuickSwitchUser={handleQuickSwitchUser}
        onLogout={handleLogout}
        mlStatus={mlHealth}
      />

      <main style={{ flex: 1 }}>
        {currentTab === 'customer' && (
          <CustomerView
            currentQueue={currentQueue}
            availableQueues={availableQueues}
            onSelectQueue={setCurrentQueue}
            currentUser={currentUser}
            onEnsureDemoQueue={ensureDemoQueue}
          />
        )}

        {currentTab === 'staff' && (
          <StaffView
            currentQueue={currentQueue}
            availableQueues={availableQueues}
            onSelectQueue={setCurrentQueue}
            onRefreshQueue={refreshCurrentQueue}
            onEnsureDemoQueue={ensureDemoQueue}
          />
        )}

        {currentTab === 'analytics' && (
          <AnalyticsView
            currentQueue={currentQueue}
            availableQueues={availableQueues}
            onSelectQueue={setCurrentQueue}
          />
        )}

        {currentTab === 'ai' && (
          <AiInspectorView />
        )}
      </main>

      <footer style={{
        textAlign: 'center',
        padding: '24px',
        color: 'var(--text-subtle)',
        fontSize: '0.8rem',
        borderTop: '1px solid var(--border-subtle)',
        marginTop: 'auto',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '8px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', flexWrap: 'wrap', justifyContent: 'center' }}>
          <span>QueueLess AI • Enterprise Clinic Management</span>
          <span>•</span>
          <button
            onClick={() => setCurrentTab(currentTab === 'ai' ? 'customer' : 'ai')}
            style={{
              background: 'transparent',
              border: 'none',
              color: 'var(--primary)',
              cursor: 'pointer',
              fontSize: '0.8rem',
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}
          >
            {currentTab === 'ai' ? '← Return to Patient View' : '🔬 Technical Specs & ML Attribution Inspector'}
          </button>
        </div>
        <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
          Spring Boot 4 Backend (:8080) • Python FastAPI ML Engine (:8000) • React 19 Client (:5173)
        </div>
      </footer>
    </div>
  );
}
