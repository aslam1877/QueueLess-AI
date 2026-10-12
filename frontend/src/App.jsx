import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import CustomerView from './components/CustomerView';
import StaffView from './components/StaffView';
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
  getMLHealth
} from './services/api';

export default function App() {
  const [currentTab, setCurrentTab] = useState('customer');
  const [currentUser, setCurrentUser] = useState(null);
  const [currentQueue, setCurrentQueue] = useState(null);
  const [mlHealth, setMlHealth] = useState(null);
  const [initializing, setInitializing] = useState(true);

  // Initialize demo queue or load current state
  const ensureDemoQueue = async () => {
    try {
      const ownerEmail = `owner_${Date.now()}@queueless.com`;
      await register({
        name: 'Dr. Sarah Lin',
        email: ownerEmail,
        password: 'Password123!',
        role: 'BUSINESS_OWNER'
      });
      await login(ownerEmail, 'Password123!');

      const biz = await createBusiness({
        name: 'Apex Diagnostic Imaging Clinic',
        address: '100 Medical Center Blvd, Suite 400',
        phone: '555-432-1098',
        description: 'Advanced MRI and diagnostic clinic'
      });

      const svc = await createService(biz.id, {
        name: 'MRI Scan & Diagnostic Review',
        description: 'Comprehensive 50-point diagnostic scan',
        estimatedDurationMinutes: 25
      });

      const q = await openQueue(svc.id, 2);
      setCurrentQueue({
        ...q,
        service: svc,
        business: biz
      });

      const custEmail = `customer_${Date.now()}@example.com`;
      await register({
        name: 'Alex Johnson',
        email: custEmail,
        password: 'Password123!',
        role: 'CUSTOMER'
      });
      const custAuth = await login(custEmail, 'Password123!');
      setCurrentUser(custAuth.user);

      return q;
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

      await ensureDemoQueue();
      setInitializing(false);
    }

    startup();
  }, []);

  const handleQuickSwitchUser = async (role) => {
    try {
      const email = `demo_${role.toLowerCase()}_${Date.now()}@example.com`;
      await register({
        name: role === 'BUSINESS_OWNER' ? 'Dr. Sarah Lin (Owner)' : 'Alex Johnson (Customer)',
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
            currentUser={currentUser}
            onEnsureDemoQueue={ensureDemoQueue}
          />
        )}

        {currentTab === 'staff' && (
          <StaffView
            currentQueue={currentQueue}
            onRefreshQueue={refreshCurrentQueue}
            onEnsureDemoQueue={ensureDemoQueue}
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
        marginTop: 'auto'
      }}>
        QueueLess AI • Spring Boot 4 Backend (Port 8080) • Python FastAPI ML Service (Port 8000) • React Vite (Port 5173)
      </footer>
    </div>
  );
}
