import React, { useState, useEffect } from 'react';
import { 
  Ticket, 
  Clock, 
  Users, 
  Sparkles, 
  AlertCircle, 
  CheckCircle, 
  RefreshCw, 
  ArrowRight, 
  XCircle,
  Activity,
  Layers,
  ChevronRight
} from 'lucide-react';
import { 
  getMyPosition, 
  joinQueue, 
  cancelMyEntry, 
  getWaitEstimate 
} from '../services/api';

export default function CustomerView({ 
  currentQueue, 
  currentUser, 
  onEnsureDemoQueue,
  availableServices 
}) {
  const [activeTicket, setActiveTicket] = useState(null);
  const [detailedEstimate, setDetailedEstimate] = useState(null);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [autoRefresh, setAutoRefresh] = useState(true);

  // Fetch customer position
  const fetchPosition = async (showSpinner = false) => {
    if (!currentQueue) return;
    if (showSpinner) setRefreshing(true);
    try {
      const pos = await getMyPosition(currentQueue.id);
      setActiveTicket(pos);
      setError(null);

      // Fetch detailed estimate breakdown
      if (pos.queueEntryId) {
        try {
          const est = await getWaitEstimate(pos.queueEntryId);
          setDetailedEstimate(est);
        } catch (e) {
          console.warn("Could not fetch detailed estimate", e);
        }
      }
    } catch (err) {
      // 404 means no active entry in queue
      setActiveTicket(null);
      setDetailedEstimate(null);
    } finally {
      if (showSpinner) setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchPosition();
  }, [currentQueue?.id, currentUser?.id]);

  // Polling loop for live position updates
  useEffect(() => {
    if (!autoRefresh || !activeTicket || !currentQueue) return;
    const interval = setInterval(() => {
      fetchPosition(false);
    }, 4000);
    return () => clearInterval(interval);
  }, [autoRefresh, activeTicket?.tokenNumber, currentQueue?.id]);

  const handleJoin = async () => {
    if (!currentQueue) {
      await onEnsureDemoQueue();
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await joinQueue(currentQueue.id);
      await fetchPosition(true);
    } catch (err) {
      setError(err.message || 'Failed to join queue');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async () => {
    if (!activeTicket?.queueEntryId) return;
    if (!window.confirm("Are you sure you want to cancel your queue ticket?")) return;
    setLoading(true);
    try {
      await cancelMyEntry(activeTicket.queueEntryId);
      setActiveTicket(null);
      setDetailedEstimate(null);
    } catch (err) {
      setError(err.message || 'Failed to cancel entry');
    } finally {
      setLoading(false);
    }
  };

  const isCalled = activeTicket?.status === 'CALLED';
  const isInService = activeTicket?.status === 'IN_SERVICE';

  return (
    <div style={{ maxWidth: '960px', margin: '0 auto', padding: '0 24px 48px 24px' }}>
      
      <div style={{ marginBottom: '28px' }}>
        <h1 style={{ fontSize: '2rem', fontWeight: 700, color: '#FFFFFF', marginBottom: '8px' }}>
          Customer Queue Portal
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem' }}>
          Track your live queue position with sub-minute AI wait-time intelligence.
        </p>
      </div>

      {error && (
        <div style={{
          background: 'var(--danger-bg)',
          border: '1px solid var(--danger-border)',
          borderRadius: '12px',
          padding: '14px 18px',
          color: '#FCA5A5',
          marginBottom: '24px',
          display: 'flex',
          alignItems: 'center',
          gap: '12px'
        }}>
          <AlertCircle size={20} color="var(--danger)" />
          <div style={{ fontSize: '0.9rem' }}>{error}</div>
        </div>
      )}

      {activeTicket ? (
        <div>
          {isCalled && (
            <div style={{
              background: 'linear-gradient(135deg, rgba(6, 182, 212, 0.25), rgba(16, 185, 129, 0.25))',
              border: '2px solid var(--primary)',
              borderRadius: '16px',
              padding: '20px 24px',
              marginBottom: '24px',
              boxShadow: '0 0 30px rgba(6, 182, 212, 0.35)',
              display: 'flex',
              alignItems: 'center',
              gap: '16px',
              animation: 'pulse-border 2s infinite'
            }}>
              <div style={{
                background: 'var(--primary)',
                width: '48px',
                height: '48px',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <Sparkles size={26} color="#090D16" />
              </div>
              <div>
                <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#FFFFFF' }}>
                  🎉 YOUR TICKET HAS BEEN CALLED!
                </h3>
                <p style={{ color: '#E0F2FE', fontSize: '0.95rem' }}>
                  Please proceed immediately to the service desk. Your counter is ready.
                </p>
              </div>
            </div>
          )}

          <div className="glass-panel" style={{
            padding: '32px',
            marginBottom: '24px',
            position: 'relative',
            overflow: 'hidden',
            border: isCalled ? '1px solid var(--primary)' : '1px solid var(--border-subtle)'
          }}>
            <div style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              height: '4px',
              background: isCalled 
                ? 'linear-gradient(90deg, #06B6D4, #10B981)' 
                : 'linear-gradient(90deg, #06B6D4, #8B5CF6)'
            }} />

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '20px' }}>
              <div>
                <span className="badge" style={{
                  background: 'rgba(255, 255, 255, 0.05)',
                  color: 'var(--text-muted)',
                  marginBottom: '10px'
                }}>
                  Live Queue Ticket
                </span>
                <h2 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#FFFFFF' }}>
                  {activeTicket.businessName || 'Medical Service'}
                </h2>
                <div style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
                  {activeTicket.serviceName || 'General Consultation'}
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <span className={`badge badge-${activeTicket.status?.toLowerCase()}`} style={{ padding: '8px 16px', fontSize: '0.85rem' }}>
                  {activeTicket.status === 'CALLED' ? '📢 CALLED NOW' : activeTicket.status}
                </span>
                <button
                  onClick={() => fetchPosition(true)}
                  disabled={refreshing}
                  title="Refresh Ticket Status"
                  style={{
                    background: 'rgba(255, 255, 255, 0.06)',
                    border: '1px solid var(--border-subtle)',
                    color: 'var(--text-main)',
                    padding: '8px',
                    borderRadius: '8px',
                    cursor: 'pointer'
                  }}
                >
                  <RefreshCw size={16} className={refreshing ? 'animate-spin' : ''} />
                </button>
              </div>
            </div>

            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
              gap: '20px',
              margin: '32px 0',
              padding: '24px',
              background: 'rgba(0, 0, 0, 0.35)',
              borderRadius: '16px',
              border: '1px solid var(--border-subtle)'
            }}>
              <div>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                  Your Token
                </div>
                <div style={{
                  fontFamily: 'var(--font-mono)',
                  fontSize: '3rem',
                  fontWeight: 800,
                  color: 'var(--primary)',
                  letterSpacing: '0.05em'
                }}>
                  {activeTicket.tokenNumber}
                </div>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                  Assigned on arrival
                </div>
              </div>

              <div>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                  Queue Position
                </div>
                <div style={{ fontSize: '3rem', fontWeight: 800, color: '#FFFFFF', fontFamily: 'var(--font-heading)' }}>
                  #{activeTicket.position}
                </div>
                <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Users size={14} color="var(--primary)" />
                  {activeTicket.peopleAhead === 0 ? (
                    <span style={{ color: 'var(--success)' }}>You are next in line!</span>
                  ) : (
                    <span><strong>{activeTicket.peopleAhead}</strong> customer(s) ahead of you</span>
                  )}
                </div>
              </div>

              <div>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                  Estimated Wait Time
                </div>
                <div style={{ fontSize: '3rem', fontWeight: 800, color: '#38BDF8', fontFamily: 'var(--font-heading)' }}>
                  {activeTicket.estimatedWaitMinutes} <span style={{ fontSize: '1.25rem', fontWeight: 600 }}>min</span>
                </div>
                <div style={{ fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  {activeTicket.estimationSource === 'ML_MODEL' ? (
                    <span className="badge badge-ai" style={{ fontSize: '0.7rem', padding: '2px 8px' }}>
                      <Sparkles size={12} /> AI ML Prediction
                    </span>
                  ) : (
                    <span className="badge badge-waiting" style={{ fontSize: '0.7rem', padding: '2px 8px' }}>
                      Deterministic Baseline
                    </span>
                  )}
                </div>
              </div>
            </div>

            {detailedEstimate && (
              <div style={{
                background: 'rgba(6, 182, 212, 0.05)',
                border: '1px solid rgba(6, 182, 212, 0.2)',
                borderRadius: '12px',
                padding: '16px 20px',
                marginBottom: '24px'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--primary)', fontWeight: 600, fontSize: '0.85rem', marginBottom: '8px' }}>
                  <Sparkles size={16} />
                  AI Wait Time Rationale
                </div>
                <p style={{ color: 'var(--text-main)', fontSize: '0.88rem', lineHeight: '1.5' }}>
                  {detailedEstimate.explanation}
                </p>
                <div style={{
                  display: 'flex',
                  gap: '24px',
                  marginTop: '12px',
                  paddingTop: '12px',
                  borderTop: '1px solid rgba(255, 255, 255, 0.06)',
                  fontSize: '0.8rem',
                  color: 'var(--text-muted)'
                }}>
                  <div>
                    Active Counters: <strong style={{ color: '#FFFFFF' }}>{detailedEstimate.activeCounters}</strong>
                  </div>
                  <div>
                    Base Service Duration: <strong style={{ color: '#FFFFFF' }}>{detailedEstimate.serviceDurationMinutes}m</strong>
                  </div>
                  {detailedEstimate.estimatedServiceTime && (
                    <div>
                      Expected Start: <strong style={{ color: '#FFFFFF' }}>
                        {new Date(detailedEstimate.estimatedServiceTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </strong>
                    </div>
                  )}
                </div>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                <span className="pulse-indicator"></span>
                <span>Auto-refreshing every 4s</span>
              </div>

              <button
                onClick={handleCancel}
                disabled={loading || isCalled || isInService}
                style={{
                  background: 'rgba(239, 68, 68, 0.1)',
                  border: '1px solid var(--danger-border)',
                  color: 'var(--danger)',
                  padding: '10px 18px',
                  borderRadius: '10px',
                  cursor: (isCalled || isInService) ? 'not-allowed' : 'pointer',
                  fontWeight: 600,
                  fontSize: '0.85rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  opacity: (isCalled || isInService) ? 0.5 : 1
                }}
              >
                <XCircle size={16} />
                Cancel Ticket & Leave Queue
              </button>
            </div>
          </div>
        </div>
      ) : (
        /* CASE 2: CUSTOMER NOT IN QUEUE (SELECT SERVICE & JOIN) */
        <div>
          <div className="glass-panel" style={{ padding: '32px', marginBottom: '32px', textAlign: 'center' }}>
            <div style={{
              width: '64px',
              height: '64px',
              borderRadius: '20px',
              background: 'linear-gradient(135deg, rgba(6, 182, 212, 0.2), rgba(139, 92, 246, 0.2))',
              border: '1px solid rgba(6, 182, 212, 0.3)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 20px auto'
            }}>
              <Ticket size={32} color="var(--primary)" />
            </div>

            <h2 style={{ fontSize: '1.6rem', fontWeight: 700, color: '#FFFFFF', marginBottom: '8px' }}>
              Join Today's Live Queue
            </h2>
            <p style={{ color: 'var(--text-muted)', maxWidth: '520px', margin: '0 auto 28px auto', fontSize: '0.95rem' }}>
              Skip physical waiting lines. Grab an instant digital token and receive precise AI-computed wait time estimates directly on your phone or browser.
            </p>

            {currentQueue ? (
              <button
                onClick={handleJoin}
                disabled={loading}
                style={{
                  background: 'linear-gradient(135deg, #06B6D4, #0891B2)',
                  border: 'none',
                  color: '#090D16',
                  padding: '16px 36px',
                  borderRadius: '12px',
                  fontSize: '1rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '10px',
                  boxShadow: '0 0 24px rgba(6, 182, 212, 0.4)',
                  transition: 'all 0.2s'
                }}
              >
                <Ticket size={20} />
                {loading ? 'Issuing Ticket...' : `Get Ticket for ${currentQueue.service?.name || 'Service'}`}
                <ArrowRight size={18} />
              </button>
            ) : (
              <div style={{
                background: 'rgba(255, 255, 255, 0.04)',
                border: '1px dashed var(--border-subtle)',
                borderRadius: '12px',
                padding: '24px',
                maxWidth: '480px',
                margin: '0 auto'
              }}>
                <div style={{ color: 'var(--warning)', fontWeight: 600, marginBottom: '8px' }}>
                  No Active Queue Open
                </div>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: '16px' }}>
                  Click below to automatically setup a sample clinic queue with active counters.
                </p>
                <button
                  onClick={onEnsureDemoQueue}
                  style={{
                    background: 'var(--primary)',
                    color: '#090D16',
                    border: 'none',
                    padding: '10px 20px',
                    borderRadius: '8px',
                    fontWeight: 700,
                    cursor: 'pointer',
                    fontSize: '0.85rem'
                  }}
                >
                  Initialize Demo Queue
                </button>
              </div>
            )}
          </div>

          <div>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 600, color: '#FFFFFF', marginBottom: '16px' }}>
              Service Capabilities & AI Features
            </h3>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px' }}>
              <div className="glass-panel" style={{ padding: '20px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
                  <Clock size={20} color="var(--primary)" />
                  <div style={{ fontWeight: 600, color: '#FFFFFF' }}>Non-Linear Rush Forecasting</div>
                </div>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  Accounts for lunch peaks, day-of-week demand, and counter fatigue rather than relying solely on naive arithmetic.
                </p>
              </div>

              <div className="glass-panel" style={{ padding: '20px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
                  <Activity size={20} color="var(--accent-purple)" />
                  <div style={{ fontWeight: 600, color: '#FFFFFF' }}>Empirical Moving Average</div>
                </div>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  Continuously adapts to actual recorded staff service completion intervals to refine base times.
                </p>
              </div>

              <div className="glass-panel" style={{ padding: '20px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
                  <CheckCircle size={20} color="var(--success)" />
                  <div style={{ fontWeight: 600, color: '#FFFFFF' }}>Deterministic Guaranteed Fallback</div>
                </div>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  Mathematical circuit breaker ensures seamless estimation even if external microservices encounter latency.
                </p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
