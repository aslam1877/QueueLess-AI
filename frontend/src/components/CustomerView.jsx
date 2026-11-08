import React, { useState, useEffect, useRef } from 'react';
import { 
  Ticket, 
  Clock, 
  Users, 
  Sparkles, 
  AlertCircle, 
  CheckCircle2, 
  RefreshCw, 
  ArrowRight, 
  XCircle,
  Activity,
  Volume2, 
  VolumeX, 
  Coffee, 
  Bell, 
  Check, 
  ShieldCheck,
  ChevronRight,
  Info
} from 'lucide-react';
import { 
  getMyPosition, 
  cancelMyEntry, 
  getWaitEstimate,
  quickJoinAsGuest 
} from '../services/api';
import { 
  playCallChime, 
  requestNotificationPermission, 
  sendBrowserNotification, 
  flashTabTitle, 
  stopFlashingTitle 
} from '../utils/audioAlert';

export default function CustomerView({ 
  currentQueue, 
  availableQueues = [], 
  onSelectQueue, 
  currentUser, 
  onEnsureDemoQueue 
}) {
  const [activeTicket, setActiveTicket] = useState(null);
  const [detailedEstimate, setDetailedEstimate] = useState(null);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [autoRefresh, setAutoRefresh] = useState(true);
  
  // Audio & Notification state
  const [soundEnabled, setSoundEnabled] = useState(true);
  const [notificationStatus, setNotificationStatus] = useState(
    typeof window !== 'undefined' && 'Notification' in window ? Notification.permission : 'unsupported'
  );
  const [guestName, setGuestName] = useState('');
  const prevStatusRef = useRef(null);

  // Determine active queue to show
  const selectedQueue = currentQueue || (availableQueues.length > 0 ? availableQueues[0] : null);

  // Check ticket in current queue or scan available queues
  const fetchPosition = async (showSpinner = false) => {
    if (!selectedQueue?.id) return;
    if (showSpinner) setRefreshing(true);
    try {
      const pos = await getMyPosition(selectedQueue.id);
      
      // Check for status transition to CALLED -> Fire Audio & Push Alerts
      if (pos && pos.status === 'CALLED' && prevStatusRef.current !== 'CALLED') {
        if (soundEnabled) {
          playCallChime();
        }
        sendBrowserNotification(`📢 Token ${pos.tokenNumber} has been called!`, {
          body: `Your counter is ready at ${pos.businessName || 'the clinic'}. Please proceed inside immediately.`
        });
        flashTabTitle(`🔔 (CALLED) ${pos.tokenNumber} Ready!`);
      } else if (pos && (pos.status === 'COMPLETED' || pos.status === 'CANCELLED')) {
        stopFlashingTitle();
      }

      prevStatusRef.current = pos?.status || null;
      setActiveTicket(pos);
      setError(null);

      // Fetch detailed estimate breakdown
      if (pos && pos.queueEntryId) {
        try {
          const est = await getWaitEstimate(pos.queueEntryId);
          setDetailedEstimate(est);
        } catch (e) {
          console.warn("Could not fetch detailed estimate", e);
        }
      }
    } catch (err) {
      // 404 means no active entry in this queue
      setActiveTicket(null);
      setDetailedEstimate(null);
      prevStatusRef.current = null;
      stopFlashingTitle();
    } finally {
      if (showSpinner) setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchPosition();
    return () => {
      stopFlashingTitle();
    };
  }, [selectedQueue?.id, currentUser?.id]);

  // Polling loop for live position updates
  useEffect(() => {
    if (!autoRefresh || !activeTicket || !selectedQueue) return;
    const interval = setInterval(() => {
      fetchPosition(false);
    }, 4000);
    return () => clearInterval(interval);
  }, [autoRefresh, activeTicket?.tokenNumber, selectedQueue?.id, soundEnabled]);

  const handleJoin = async (targetQueueId) => {
    const queueToJoin = targetQueueId || selectedQueue?.id;
    if (!queueToJoin) {
      await onEnsureDemoQueue();
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await quickJoinAsGuest(queueToJoin, guestName);
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
      stopFlashingTitle();
    } catch (err) {
      setError(err.message || 'Failed to cancel entry');
    } finally {
      setLoading(false);
    }
  };

  const handleEnableNotifications = async () => {
    const res = await requestNotificationPermission();
    setNotificationStatus(res);
    if (res === 'granted') {
      sendBrowserNotification("✅ Notifications Enabled!", {
        body: "You can safely minimize this tab. We will alert you the moment your token is called."
      });
    }
  };

  const handleTestChime = () => {
    playCallChime();
  };

  const isCalled = activeTicket?.status === 'CALLED';
  const isInService = activeTicket?.status === 'IN_SERVICE';

  // Use available queues if loaded, else wrap selectedQueue
  const displayQueues = availableQueues.length > 0 ? availableQueues : (selectedQueue ? [selectedQueue] : []);

  return (
    <div style={{ maxWidth: '860px', margin: '0 auto', padding: '0 20px 48px 20px' }}>
      
      {/* Top Welcome Bar & Audio Alert Controls */}
      <div style={{ 
        marginBottom: '24px', 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        flexWrap: 'wrap', 
        gap: '16px' 
      }}>
        <div>
          <div style={{ 
            display: 'inline-flex', 
            alignItems: 'center', 
            gap: '6px', 
            background: 'rgba(6, 182, 212, 0.1)', 
            border: '1px solid rgba(6, 182, 212, 0.25)',
            padding: '4px 10px', 
            borderRadius: '20px', 
            fontSize: '0.75rem', 
            fontWeight: 600, 
            color: 'var(--primary)',
            marginBottom: '8px'
          }}>
            <span className="pulse-indicator"></span>
            Apex Diagnostic Imaging Clinic • Live Queue
          </div>
          <h1 style={{ fontSize: '1.85rem', fontWeight: 800, color: '#FFFFFF', margin: 0, letterSpacing: '-0.02em' }}>
            {activeTicket ? 'Your Active Queue Ticket' : 'Digital Check-in & Waiting Line'}
          </h1>
        </div>

        {/* Chime & Notification Controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
          <button
            onClick={() => setSoundEnabled(!soundEnabled)}
            title={soundEnabled ? 'Chime sound is active' : 'Chime sound is muted'}
            style={{
              background: soundEnabled ? 'rgba(6, 182, 212, 0.15)' : 'rgba(255, 255, 255, 0.05)',
              border: `1px solid ${soundEnabled ? 'var(--primary)' : 'var(--border-subtle)'}`,
              color: soundEnabled ? 'var(--primary)' : 'var(--text-muted)',
              padding: '8px 14px',
              borderRadius: '10px',
              cursor: 'pointer',
              fontSize: '0.8rem',
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}
          >
            {soundEnabled ? <Volume2 size={15} /> : <VolumeX size={15} />}
            {soundEnabled ? 'Chime ON' : 'Muted'}
          </button>

          {soundEnabled && (
            <button
              onClick={handleTestChime}
              title="Test the call chime volume"
              style={{
                background: 'rgba(255, 255, 255, 0.05)',
                border: '1px solid var(--border-subtle)',
                color: 'var(--text-muted)',
                padding: '8px 12px',
                borderRadius: '10px',
                cursor: 'pointer',
                fontSize: '0.78rem'
              }}
            >
              Test Chime 🔔
            </button>
          )}

          {notificationStatus !== 'granted' && (
            <button
              onClick={handleEnableNotifications}
              style={{
                background: 'rgba(139, 92, 246, 0.15)',
                border: '1px solid rgba(139, 92, 246, 0.3)',
                color: '#A78BFA',
                padding: '8px 14px',
                borderRadius: '10px',
                cursor: 'pointer',
                fontSize: '0.8rem',
                fontWeight: 600,
                display: 'flex',
                alignItems: 'center',
                gap: '6px'
              }}
            >
              <Bell size={15} />
              Enable Alerts
            </button>
          )}
        </div>
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

      {/* ======================================================== */}
      {/* SCENARIO 1: PATIENT HAS AN ACTIVE TICKET                */}
      {/* ======================================================== */}
      {activeTicket ? (
        <div>
          {/* CALL ALERT HERO BANNER */}
          {isCalled && (
            <div style={{
              background: 'linear-gradient(135deg, rgba(6, 182, 212, 0.35), rgba(16, 185, 129, 0.35))',
              border: '2px solid var(--primary)',
              borderRadius: '16px',
              padding: '24px',
              marginBottom: '24px',
              boxShadow: '0 0 35px rgba(6, 182, 212, 0.5)',
              display: 'flex',
              alignItems: 'center',
              gap: '18px',
              animation: 'pulse-border 2s infinite'
            }}>
              <div style={{
                background: 'var(--primary)',
                width: '56px',
                height: '56px',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0
              }}>
                <Sparkles size={30} color="#090D16" />
              </div>
              <div>
                <h3 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#FFFFFF', marginBottom: '4px' }}>
                  📢 TICKET {activeTicket.tokenNumber} IS CALLED!
                </h3>
                <p style={{ color: '#E0F2FE', fontSize: '1rem', margin: 0 }}>
                  Please proceed inside now to Counter 1 / Consultation Desk. Your practitioner is ready!
                </p>
              </div>
            </div>
          )}

          {/* MAIN DIGITAL TICKET CARD */}
          <div className="glass-panel" style={{
            padding: '32px',
            marginBottom: '24px',
            position: 'relative',
            overflow: 'hidden',
            border: isCalled ? '2px solid var(--primary)' : '1px solid var(--border-subtle)'
          }}>
            <div style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              height: '5px',
              background: isCalled 
                ? 'linear-gradient(90deg, #06B6D4, #10B981)' 
                : 'linear-gradient(90deg, #06B6D4, #8B5CF6)'
            }} />

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
              <div>
                <div style={{ fontSize: '0.8rem', color: 'var(--primary)', fontWeight: 600, textTransform: 'uppercase', marginBottom: '4px' }}>
                  {activeTicket.businessName || 'Apex Diagnostic Imaging Clinic'}
                </div>
                <h2 style={{ fontSize: '1.6rem', fontWeight: 800, color: '#FFFFFF', margin: 0 }}>
                  {activeTicket.serviceName || 'Consultation & Review'}
                </h2>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <span className={`badge badge-${activeTicket.status?.toLowerCase()}`} style={{ padding: '8px 16px', fontSize: '0.88rem', fontWeight: 700 }}>
                  {activeTicket.status === 'CALLED' ? '📢 CALLED NOW' : activeTicket.status === 'IN_SERVICE' ? '🩺 IN CONSULTATION' : '⏳ WAITING IN LINE'}
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

            {/* Main Stats: Token, Position, Wait Time */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))',
              gap: '20px',
              margin: '28px 0',
              padding: '24px',
              background: 'rgba(0, 0, 0, 0.35)',
              borderRadius: '16px',
              border: '1px solid var(--border-subtle)'
            }}>
              <div>
                <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                  Your Ticket
                </div>
                <div style={{
                  fontFamily: 'var(--font-mono)',
                  fontSize: '3.4rem',
                  fontWeight: 800,
                  color: 'var(--primary)',
                  letterSpacing: '0.05em',
                  lineHeight: '1.1'
                }}>
                  {activeTicket.tokenNumber}
                </div>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)', marginTop: '4px' }}>
                  Assigned upon arrival
                </div>
              </div>

              <div>
                <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                  Position in Line
                </div>
                <div style={{ fontSize: '3.4rem', fontWeight: 800, color: '#FFFFFF', fontFamily: 'var(--font-heading)', lineHeight: '1.1' }}>
                  #{activeTicket.position}
                </div>
                <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '4px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Users size={14} color="var(--primary)" />
                  {activeTicket.peopleAhead === 0 ? (
                    <span style={{ color: 'var(--success)', fontWeight: 600 }}>You are next in line!</span>
                  ) : (
                    <span><strong>{activeTicket.peopleAhead}</strong> {activeTicket.peopleAhead === 1 ? 'person' : 'people'} ahead</span>
                  )}
                </div>
              </div>

              <div>
                <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                  Estimated Wait
                </div>
                <div style={{ fontSize: '3.4rem', fontWeight: 800, color: '#38BDF8', fontFamily: 'var(--font-heading)', lineHeight: '1.1' }}>
                  ~{activeTicket.estimatedWaitMinutes} <span style={{ fontSize: '1.2rem', fontWeight: 600 }}>min</span>
                </div>
                <div style={{ fontSize: '0.8rem', marginTop: '4px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <span className="badge badge-ai" style={{ fontSize: '0.72rem', padding: '2px 8px' }}>
                    <Sparkles size={12} /> Live Dynamic Estimate
                  </span>
                </div>
              </div>
            </div>

            {/* Reassuring "Step Away" Guidance Box */}
            <div style={{
              background: 'rgba(16, 185, 129, 0.08)',
              border: '1px solid rgba(16, 185, 129, 0.25)',
              borderRadius: '12px',
              padding: '16px 20px',
              marginBottom: '24px',
              display: 'flex',
              alignItems: 'center',
              gap: '16px'
            }}>
              <div style={{
                background: 'rgba(16, 185, 129, 0.2)',
                width: '42px',
                height: '42px',
                borderRadius: '10px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0
              }}>
                <Coffee size={22} color="var(--success)" />
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ color: '#FFFFFF', fontWeight: 700, fontSize: '0.92rem', marginBottom: '2px' }}>
                  You don't need to stay in the waiting room!
                </div>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.84rem', margin: 0, lineHeight: '1.4' }}>
                  Feel free to step outside, get a coffee, or wait comfortably in your vehicle. Keep this browser tab open—an audible chime will sound when your number is called.
                </p>
              </div>
            </div>

            {/* Bottom Actions & Status */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                <span className="pulse-indicator"></span>
                <span>Live updates every 4 seconds • Chime ready</span>
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
                Cancel Ticket & Leave Line
              </button>
            </div>
          </div>
        </div>
      ) : (
        /* ======================================================== */
        /* SCENARIO 2: NO TICKET YET -> 2-STEP SERVICE JOIN FLOW     */
        /* ======================================================== */
        <div>
          
          {/* STEP 1: SERVICE PICKER */}
          <div style={{ marginBottom: '28px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
              <div style={{ 
                background: 'var(--primary)', 
                color: '#090D16', 
                width: '24px', 
                height: '24px', 
                borderRadius: '50%', 
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'center', 
                fontWeight: 800, 
                fontSize: '0.8rem' 
              }}>
                1
              </div>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#FFFFFF', margin: 0 }}>
                Select What You Need Today:
              </h2>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '14px' }}>
              {displayQueues.map((q) => {
                const isSelected = selectedQueue?.id === q.id;
                const serviceName = q.service?.name || 'General Consultation';
                const duration = q.service?.estimatedDurationMinutes || 15;
                const counters = q.activeCounters || 2;

                return (
                  <div
                    key={q.id}
                    onClick={() => onSelectQueue && onSelectQueue(q)}
                    style={{
                      background: isSelected 
                        ? 'linear-gradient(135deg, rgba(6, 182, 212, 0.18), rgba(15, 23, 42, 0.8))' 
                        : 'rgba(255, 255, 255, 0.03)',
                      border: isSelected ? '2px solid var(--primary)' : '1px solid var(--border-subtle)',
                      borderRadius: '14px',
                      padding: '20px',
                      cursor: 'pointer',
                      transition: 'all 0.2s',
                      position: 'relative'
                    }}
                  >
                    {isSelected && (
                      <div style={{
                        position: 'absolute',
                        top: '12px',
                        right: '12px',
                        background: 'var(--primary)',
                        color: '#090D16',
                        borderRadius: '50%',
                        width: '20px',
                        height: '20px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}>
                        <Check size={14} strokeWidth={3} />
                      </div>
                    )}

                    <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#FFFFFF', marginBottom: '6px', paddingRight: '24px' }}>
                      {serviceName}
                    </div>

                    <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '14px', lineHeight: '1.4' }}>
                      {q.service?.description || 'Primary doctor consultation & vitals'}
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', fontSize: '0.78rem', color: isSelected ? 'var(--primary)' : 'var(--text-subtle)' }}>
                      <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Clock size={13} /> ~{duration} min avg
                      </span>
                      <span>•</span>
                      <span>{counters} {counters === 1 ? 'desk' : 'desks'} open</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* STEP 2: GUEST NAME & INSTANT JOIN */}
          <div className="glass-panel" style={{ padding: '28px', marginBottom: '32px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
              <div style={{ 
                background: 'var(--primary)', 
                color: '#090D16', 
                width: '24px', 
                height: '24px', 
                borderRadius: '50%', 
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'center', 
                fontWeight: 800, 
                fontSize: '0.8rem' 
              }}>
                2
              </div>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#FFFFFF', margin: 0 }}>
                Get Your Digital Ticket:
              </h2>
            </div>

            {selectedQueue ? (
              <div style={{ maxWidth: '500px' }}>
                <div style={{ marginBottom: '16px' }}>
                  <label style={{ display: 'block', fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '6px', fontWeight: 600 }}>
                    Your Name (Optional):
                  </label>
                  <input
                    type="text"
                    value={guestName}
                    onChange={(e) => setGuestName(e.target.value)}
                    placeholder="e.g., Alex Johnson"
                    style={{
                      width: '100%',
                      background: 'rgba(255, 255, 255, 0.06)',
                      border: '1px solid var(--border-subtle)',
                      borderRadius: '10px',
                      padding: '12px 16px',
                      color: '#FFFFFF',
                      fontSize: '0.95rem',
                      outline: 'none'
                    }}
                  />
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', marginTop: '4px' }}>
                    No account or password required. Ticket is linked to this session.
                  </div>
                </div>

                <button
                  onClick={() => handleJoin(selectedQueue.id)}
                  disabled={loading}
                  style={{
                    width: '100%',
                    background: 'linear-gradient(135deg, #06B6D4, #0891B2)',
                    border: 'none',
                    color: '#090D16',
                    padding: '16px',
                    borderRadius: '12px',
                    fontSize: '1.05rem',
                    fontWeight: 800,
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '10px',
                    boxShadow: '0 0 24px rgba(6, 182, 212, 0.35)',
                    transition: 'all 0.2s'
                  }}
                >
                  <Ticket size={20} />
                  {loading ? 'Issuing Ticket...' : `Get Ticket for ${selectedQueue.service?.name || 'Selected Service'}`}
                  <ArrowRight size={18} />
                </button>
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: '20px' }}>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
                  No active clinic queues available.
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
                  Open Today's Clinic Queues
                </button>
              </div>
            )}
          </div>

          {/* USER BENEFITS (Plain English, zero ML jargon) */}
          <div>
            <h3 style={{ fontSize: '1.05rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '14px', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              How It Works:
            </h3>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px' }}>
              <div className="glass-panel" style={{ padding: '20px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
                  <Clock size={18} color="var(--primary)" />
                  <div style={{ fontWeight: 700, color: '#FFFFFF', fontSize: '0.95rem' }}>Live Wait Estimates</div>
                </div>
                <p style={{ fontSize: '0.84rem', color: 'var(--text-muted)', margin: 0, lineHeight: '1.4' }}>
                  No guessing. Minutes update in real time based on active clinic counters and consultation speed.
                </p>
              </div>

              <div className="glass-panel" style={{ padding: '20px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
                  <Coffee size={18} color="var(--success)" />
                  <div style={{ fontWeight: 700, color: '#FFFFFF', fontSize: '0.95rem' }}>Freedom to Step Away</div>
                </div>
                <p style={{ fontSize: '0.84rem', color: 'var(--text-muted)', margin: 0, lineHeight: '1.4' }}>
                  Grab coffee nearby or wait in your car. Your device will chime when your ticket is announced.
                </p>
              </div>

              <div className="glass-panel" style={{ padding: '20px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
                  <ShieldCheck size={18} color="#A78BFA" />
                  <div style={{ fontWeight: 700, color: '#FFFFFF', fontSize: '0.95rem' }}>Zero Account Hassle</div>
                </div>
                <p style={{ fontSize: '0.84rem', color: 'var(--text-muted)', margin: 0, lineHeight: '1.4' }}>
                  No app download, no password, and no spam. Just grab your digital token and proceed.
                </p>
              </div>
            </div>
          </div>

        </div>
      )}

    </div>
  );
}
