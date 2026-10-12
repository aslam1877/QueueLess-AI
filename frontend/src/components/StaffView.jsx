import React, { useState, useEffect } from 'react';
import { 
  Megaphone, 
  Play, 
  CheckCircle2, 
  SkipForward, 
  Users, 
  Clock, 
  RefreshCw, 
  AlertCircle,
  PlusCircle,
  CheckCircle,
  Sliders,
  Power
} from 'lucide-react';
import { 
  getQueueEntries, 
  callNext, 
  startService, 
  completeService, 
  skipEntry,
  joinQueue,
  closeQueue,
  openQueue
} from '../services/api';

export default function StaffView({ 
  currentQueue, 
  onRefreshQueue, 
  onEnsureDemoQueue 
}) {
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);

  const fetchEntries = async () => {
    if (!currentQueue?.id) return;
    try {
      const data = await getQueueEntries(currentQueue.id);
      setEntries(data || []);
      setError(null);
    } catch (err) {
      console.warn("Could not fetch entries", err);
    }
  };

  useEffect(() => {
    fetchEntries();
    const interval = setInterval(fetchEntries, 3500);
    return () => clearInterval(interval);
  }, [currentQueue?.id]);

  const handleCallNext = async () => {
    if (!currentQueue?.id) return;
    setActionLoading(true);
    setError(null);
    try {
      const called = await callNext(currentQueue.id);
      setMessage(`📢 Called Token ${called.tokenNumber}!`);
      await fetchEntries();
      await onRefreshQueue();
    } catch (err) {
      setError(err.message || 'No more customers waiting to call');
    } finally {
      setActionLoading(false);
    }
  };

  const handleStartService = async (id, token) => {
    setActionLoading(true);
    try {
      await startService(id);
      setMessage(`Started service for Token ${token}`);
      await fetchEntries();
    } catch (err) {
      setError(err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleCompleteService = async (id, token) => {
    setActionLoading(true);
    try {
      await completeService(id);
      setMessage(`Completed service for Token ${token}! Duration recorded for AI training.`);
      await fetchEntries();
      await onRefreshQueue();
    } catch (err) {
      setError(err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleSkip = async (id, token) => {
    setActionLoading(true);
    try {
      await skipEntry(id);
      setMessage(`Skipped no-show Token ${token}`);
      await fetchEntries();
    } catch (err) {
      setError(err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleAddTestCustomer = async () => {
    if (!currentQueue?.id) return;
    setActionLoading(true);
    try {
      await joinQueue(currentQueue.id);
      setMessage("Added test customer into queue!");
      await fetchEntries();
      await onRefreshQueue();
    } catch (err) {
      setError(err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const waitingCount = entries.filter(e => e.status === 'WAITING').length;
  const inServiceCount = entries.filter(e => e.status === 'IN_SERVICE').length;
  const calledEntry = entries.find(e => e.status === 'CALLED');
  const inServiceEntry = entries.find(e => e.status === 'IN_SERVICE');

  return (
    <div style={{ maxWidth: '1100px', margin: '0 auto', padding: '0 24px 48px 24px' }}>
      
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px', marginBottom: '28px' }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontWeight: 700, color: '#FFFFFF', marginBottom: '8px' }}>
            Staff Command Center
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem' }}>
            Manage queue flow, announce tickets, and log service completions that power the ML model.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={handleAddTestCustomer}
            disabled={actionLoading || !currentQueue?.id}
            style={{
              background: 'rgba(6, 182, 212, 0.12)',
              border: '1px solid rgba(6, 182, 212, 0.3)',
              color: 'var(--primary)',
              padding: '10px 16px',
              borderRadius: '10px',
              fontSize: '0.85rem',
              fontWeight: 600,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}
          >
            <PlusCircle size={16} />
            + Simulate Waiting Customer
          </button>

          <button
            onClick={fetchEntries}
            style={{
              background: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid var(--border-subtle)',
              color: 'var(--text-main)',
              padding: '10px 14px',
              borderRadius: '10px',
              cursor: 'pointer'
            }}
          >
            <RefreshCw size={16} />
          </button>
        </div>
      </div>

      {message && (
        <div style={{
          background: 'var(--success-bg)',
          border: '1px solid var(--success-border)',
          borderRadius: '12px',
          padding: '12px 18px',
          color: '#6EE7B7',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          fontSize: '0.9rem'
        }}>
          <CheckCircle2 size={18} color="var(--success)" />
          {message}
        </div>
      )}

      {error && (
        <div style={{
          background: 'var(--danger-bg)',
          border: '1px solid var(--danger-border)',
          borderRadius: '12px',
          padding: '12px 18px',
          color: '#FCA5A5',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          fontSize: '0.9rem'
        }}>
          <AlertCircle size={18} color="var(--danger)" />
          {error}
        </div>
      )}

      <div className="glass-panel" style={{
        padding: '28px',
        marginBottom: '28px',
        background: 'linear-gradient(135deg, rgba(17, 24, 39, 0.8), rgba(15, 23, 42, 0.9))'
      }}>
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
          gap: '24px',
          alignItems: 'center'
        }}>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600, marginBottom: '8px' }}>
              Next Customer Calling
            </div>
            <button
              onClick={handleCallNext}
              disabled={actionLoading || waitingCount === 0 || !currentQueue?.id}
              style={{
                width: '100%',
                background: waitingCount > 0 
                  ? 'linear-gradient(135deg, #06B6D4, #0891B2)' 
                  : 'rgba(255, 255, 255, 0.08)',
                color: waitingCount > 0 ? '#090D16' : 'var(--text-subtle)',
                border: 'none',
                padding: '18px 24px',
                borderRadius: '14px',
                fontSize: '1.15rem',
                fontWeight: 800,
                cursor: waitingCount > 0 ? 'pointer' : 'not-allowed',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '12px',
                boxShadow: waitingCount > 0 ? '0 0 24px rgba(6, 182, 212, 0.4)' : 'none',
                transition: 'all 0.2s'
              }}
            >
              <Megaphone size={22} />
              CALL NEXT CUSTOMER
              {waitingCount > 0 && (
                <span style={{
                  background: '#090D16',
                  color: 'var(--primary)',
                  padding: '2px 8px',
                  borderRadius: '12px',
                  fontSize: '0.8rem',
                  fontWeight: 700
                }}>
                  {waitingCount} waiting
                </span>
              )}
            </button>
            <div style={{ fontSize: '0.78rem', color: 'var(--text-subtle)', marginTop: '8px', textAlign: 'center' }}>
              Atomically claims earliest WAITING token in FIFO order
            </div>
          </div>

          <div style={{
            background: 'rgba(0, 0, 0, 0.3)',
            borderRadius: '12px',
            padding: '18px',
            border: '1px solid var(--border-subtle)',
            display: 'flex',
            justifyContent: 'space-around',
            textAlign: 'center'
          }}>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Waiting</div>
              <div style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--warning)', fontFamily: 'var(--font-heading)' }}>
                {waitingCount}
              </div>
            </div>
            <div style={{ width: '1px', background: 'var(--border-subtle)' }} />
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>In Service</div>
              <div style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--accent-purple)', fontFamily: 'var(--font-heading)' }}>
                {inServiceCount}
              </div>
            </div>
            <div style={{ width: '1px', background: 'var(--border-subtle)' }} />
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Active Counters</div>
              <div style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--primary)', fontFamily: 'var(--font-heading)' }}>
                {currentQueue?.activeCounters || 2}
              </div>
            </div>
          </div>
        </div>

        {(calledEntry || inServiceEntry) && (
          <div style={{
            marginTop: '24px',
            paddingTop: '20px',
            borderTop: '1px solid var(--border-subtle)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '16px'
          }}>
            {calledEntry && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                <span className="badge badge-called" style={{ padding: '6px 12px' }}>
                  📢 CALLED NOW
                </span>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: '1.5rem', fontWeight: 800, color: 'var(--primary)' }}>
                  {calledEntry.tokenNumber}
                </span>
                <span style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
                  Customer: <strong>{calledEntry.customerName || 'Customer'}</strong>
                </span>
                <button
                  onClick={() => handleStartService(calledEntry.id, calledEntry.tokenNumber)}
                  disabled={actionLoading}
                  style={{
                    background: 'var(--accent-purple)',
                    color: '#FFFFFF',
                    border: 'none',
                    padding: '8px 16px',
                    borderRadius: '8px',
                    fontWeight: 700,
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    fontSize: '0.85rem'
                  }}
                >
                  <Play size={14} /> Start Service
                </button>
                <button
                  onClick={() => handleSkip(calledEntry.id, calledEntry.tokenNumber)}
                  disabled={actionLoading}
                  style={{
                    background: 'rgba(107, 114, 128, 0.2)',
                    color: 'var(--text-muted)',
                    border: '1px solid var(--border-subtle)',
                    padding: '8px 14px',
                    borderRadius: '8px',
                    fontWeight: 600,
                    cursor: 'pointer',
                    fontSize: '0.85rem'
                  }}
                >
                  Skip No-Show
                </button>
              </div>
            )}

            {inServiceEntry && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                <span className="badge badge-in_service" style={{ padding: '6px 12px' }}>
                  ⚙️ IN SERVICE
                </span>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: '1.5rem', fontWeight: 800, color: '#A78BFA' }}>
                  {inServiceEntry.tokenNumber}
                </span>
                <span style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
                  Customer: <strong>{inServiceEntry.customerName || 'Customer'}</strong>
                </span>
                <button
                  onClick={() => handleCompleteService(inServiceEntry.id, inServiceEntry.tokenNumber)}
                  disabled={actionLoading}
                  style={{
                    background: 'var(--success)',
                    color: '#090D16',
                    border: 'none',
                    padding: '8px 18px',
                    borderRadius: '8px',
                    fontWeight: 700,
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    fontSize: '0.85rem'
                  }}
                >
                  <CheckCircle size={16} /> Complete Service
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      <div className="glass-panel" style={{ padding: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#FFFFFF' }}>
            Live Queue Roster ({entries.length} Total Today)
          </h2>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
            Ordered by Joined Time (FIFO)
          </span>
        </div>

        {entries.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--text-muted)' }}>
            No tickets in queue yet. Use "Simulate Waiting Customer" above or join from Customer View!
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-subtle)', color: 'var(--text-muted)' }}>
                  <th style={{ padding: '12px 16px' }}>TOKEN</th>
                  <th style={{ padding: '12px 16px' }}>CUSTOMER</th>
                  <th style={{ padding: '12px 16px' }}>STATUS</th>
                  <th style={{ padding: '12px 16px' }}>EST. WAIT</th>
                  <th style={{ padding: '12px 16px' }}>SOURCE</th>
                  <th style={{ padding: '12px 16px' }}>JOINED AT</th>
                  <th style={{ padding: '12px 16px', textAlign: 'right' }}>ACTIONS</th>
                </tr>
              </thead>
              <tbody>
                {entries.map((item) => (
                  <tr key={item.id} style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.04)' }}>
                    <td style={{ padding: '12px 16px', fontFamily: 'var(--font-mono)', fontWeight: 700, color: 'var(--primary)' }}>
                      {item.tokenNumber}
                    </td>
                    <td style={{ padding: '12px 16px', color: '#FFFFFF', fontWeight: 500 }}>
                      {item.customerName || `Customer #${item.customerId}`}
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <span className={`badge badge-${item.status?.toLowerCase()}`}>
                        {item.status}
                      </span>
                    </td>
                    <td style={{ padding: '12px 16px', color: 'var(--text-main)' }}>
                      {item.estimatedWaitMinutes}m
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      {item.estimationSource === 'ML_MODEL' ? (
                        <span className="badge badge-ai" style={{ fontSize: '0.65rem' }}>AI Model</span>
                      ) : (
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>Baseline</span>
                      )}
                    </td>
                    <td style={{ padding: '12px 16px', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                      {item.joinedAt ? new Date(item.joinedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '—'}
                    </td>
                    <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                      {item.status === 'CALLED' && (
                        <button
                          onClick={() => handleStartService(item.id, item.tokenNumber)}
                          style={{
                            background: 'var(--accent-purple)',
                            color: '#FFF',
                            border: 'none',
                            padding: '4px 10px',
                            borderRadius: '6px',
                            fontSize: '0.75rem',
                            fontWeight: 600,
                            cursor: 'pointer',
                            marginRight: '6px'
                          }}
                        >
                          Start
                        </button>
                      )}
                      {item.status === 'IN_SERVICE' && (
                        <button
                          onClick={() => handleCompleteService(item.id, item.tokenNumber)}
                          style={{
                            background: 'var(--success)',
                            color: '#090D16',
                            border: 'none',
                            padding: '4px 10px',
                            borderRadius: '6px',
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            cursor: 'pointer'
                          }}
                        >
                          Complete
                        </button>
                      )}
                      {(item.status === 'WAITING' || item.status === 'CALLED') && (
                        <button
                          onClick={() => handleSkip(item.id, item.tokenNumber)}
                          style={{
                            background: 'transparent',
                            color: 'var(--text-subtle)',
                            border: '1px solid var(--border-subtle)',
                            padding: '4px 8px',
                            borderRadius: '6px',
                            fontSize: '0.75rem',
                            cursor: 'pointer',
                            marginLeft: '4px'
                          }}
                        >
                          Skip
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
