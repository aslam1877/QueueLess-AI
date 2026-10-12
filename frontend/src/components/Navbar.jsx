import React from 'react';
import { Sparkles, Users, LayoutDashboard, Brain, LogIn, LogOut, CheckCircle2 } from 'lucide-react';

export default function Navbar({ 
  currentTab, 
  setCurrentTab, 
  currentUser, 
  onQuickSwitchUser, 
  onLogout,
  mlStatus
}) {
  return (
    <header className="navbar-container glass-panel" style={{ margin: '16px 24px', padding: '12px 24px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{
            width: '40px',
            height: '40px',
            borderRadius: '12px',
            background: 'linear-gradient(135deg, #06B6D4, #8B5CF6)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 0 16px rgba(6, 182, 212, 0.4)'
          }}>
            <Sparkles size={22} color="#FFFFFF" />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span className="brand-title" style={{ fontSize: '1.25rem', fontWeight: 800, color: '#FFFFFF', letterSpacing: '-0.02em' }}>
                QueueLess <span style={{ color: 'var(--primary)' }}>AI</span>
              </span>
              <span className="badge badge-ai" style={{ fontSize: '0.65rem' }}>
                <span className="pulse-indicator"></span> ML Live
              </span>
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              Intelligent Wait-Time Prediction System
            </div>
          </div>
        </div>

        <nav style={{ display: 'flex', gap: '8px', background: 'rgba(0,0,0,0.3)', padding: '4px', borderRadius: '12px', border: '1px solid var(--border-subtle)' }}>
          <button
            onClick={() => setCurrentTab('customer')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '8px 16px',
              borderRadius: '8px',
              border: 'none',
              cursor: 'pointer',
              fontWeight: 600,
              fontSize: '0.85rem',
              transition: 'all 0.2s',
              background: currentTab === 'customer' ? 'var(--primary)' : 'transparent',
              color: currentTab === 'customer' ? '#090D16' : 'var(--text-muted)'
            }}
          >
            <Users size={16} />
            Customer Portal
          </button>

          <button
            onClick={() => setCurrentTab('staff')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '8px 16px',
              borderRadius: '8px',
              border: 'none',
              cursor: 'pointer',
              fontWeight: 600,
              fontSize: '0.85rem',
              transition: 'all 0.2s',
              background: currentTab === 'staff' ? 'var(--primary)' : 'transparent',
              color: currentTab === 'staff' ? '#090D16' : 'var(--text-muted)'
            }}
          >
            <LayoutDashboard size={16} />
            Staff Command Center
          </button>

          <button
            onClick={() => setCurrentTab('ai')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '8px 16px',
              borderRadius: '8px',
              border: 'none',
              cursor: 'pointer',
              fontWeight: 600,
              fontSize: '0.85rem',
              transition: 'all 0.2s',
              background: currentTab === 'ai' ? 'var(--primary)' : 'transparent',
              color: currentTab === 'ai' ? '#090D16' : 'var(--text-muted)'
            }}
          >
            <Brain size={16} />
            AI vs Baseline Inspector
          </button>
        </nav>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {currentUser ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#FFFFFF' }}>
                  {currentUser.name}
                </div>
                <div style={{ fontSize: '0.7rem', color: 'var(--primary)' }}>
                  {currentUser.role}
                </div>
              </div>
              <button
                onClick={onLogout}
                title="Logout"
                style={{
                  background: 'rgba(239, 68, 68, 0.1)',
                  border: '1px solid var(--danger-border)',
                  color: 'var(--danger)',
                  padding: '6px 10px',
                  borderRadius: '8px',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                  fontSize: '0.75rem',
                  fontWeight: 600
                }}
              >
                <LogOut size={14} />
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                onClick={() => onQuickSwitchUser('CUSTOMER')}
                style={{
                  background: 'rgba(6, 182, 212, 0.15)',
                  border: '1px solid rgba(6, 182, 212, 0.3)',
                  color: 'var(--primary)',
                  padding: '6px 12px',
                  borderRadius: '8px',
                  cursor: 'pointer',
                  fontSize: '0.8rem',
                  fontWeight: 600
                }}
              >
                Demo as Customer
              </button>
              <button
                onClick={() => onQuickSwitchUser('BUSINESS_OWNER')}
                style={{
                  background: 'rgba(139, 92, 246, 0.15)',
                  border: '1px solid rgba(139, 92, 246, 0.3)',
                  color: '#A78BFA',
                  padding: '6px 12px',
                  borderRadius: '8px',
                  cursor: 'pointer',
                  fontSize: '0.8rem',
                  fontWeight: 600
                }}
              >
                Demo as Staff/Owner
              </button>
            </div>
          )}
        </div>

      </div>
    </header>
  );
}
