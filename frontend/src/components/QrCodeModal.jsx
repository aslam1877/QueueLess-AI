import React, { useState, useEffect } from 'react';
import { X, Copy, Check, Printer, QrCode, ExternalLink, Sparkles } from 'lucide-react';
import { generateQRCodeDataURL } from '../utils/qrCode';

export default function QrCodeModal({ queue, onClose }) {
  const [qrDataUrl, setQrDataUrl] = useState(null);
  const [copied, setCopied] = useState(false);

  const joinUrl = `${window.location.origin}/?queue=${queue?.id || ''}`;

  useEffect(() => {
    async function loadQR() {
      if (!queue?.id) return;
      const dataUrl = await generateQRCodeDataURL(joinUrl, { width: 340 });
      setQrDataUrl(dataUrl);
    }
    loadQR();
  }, [queue?.id, joinUrl]);

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(joinUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    } catch (e) {
      alert(`Link: ${joinUrl}`);
    }
  };

  const handlePrint = () => {
    const printWindow = window.open('', '_blank');
    if (!printWindow) {
      alert("Please allow popups to print the queue sign.");
      return;
    }
    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
      <head>
        <title>Queue Sign - ${queue?.business?.name || 'Clinic'}</title>
        <style>
          body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            text-align: center;
            padding: 40px;
            color: #0f172a;
          }
          .card {
            border: 4px solid #06b6d4;
            border-radius: 24px;
            padding: 48px;
            max-width: 600px;
            margin: 0 auto;
          }
          h1 { font-size: 2.2rem; margin: 0 0 8px 0; color: #0891b2; }
          h2 { font-size: 1.4rem; margin: 0 0 24px 0; color: #334155; }
          .qr-img { width: 280px; height: 280px; margin: 16px auto; }
          .instructions { font-size: 1.2rem; font-weight: 600; color: #0f172a; margin-top: 24px; }
          .subtext { font-size: 0.95rem; color: #64748b; margin-top: 8px; }
          .brand { margin-top: 32px; font-size: 0.85rem; color: #94a3b8; letter-spacing: 0.05em; }
        </style>
      </head>
      <body>
        <div class="card">
          <h1>${queue?.business?.name || 'Medical Services'}</h1>
          <h2>${queue?.service?.name || 'General Consultation'}</h2>
          <div>
            <img class="qr-img" src="${qrDataUrl}" alt="Scan to join queue" />
          </div>
          <div class="instructions">📱 Scan with phone camera to join line!</div>
          <div class="subtext">Get an instant digital token with real-time AI wait tracking. You don't have to wait in this room.</div>
          <div class="brand">POWERED BY QUEUELESS AI</div>
        </div>
        <script>
          window.onload = function() { window.print(); }
        </script>
      </body>
      </html>
    `);
    printWindow.document.close();
  };

  return (
    <div style={{
      position: 'fixed',
      inset: 0,
      background: 'rgba(0, 0, 0, 0.75)',
      backdropFilter: 'blur(8px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 9999,
      padding: '20px'
    }}>
      <div className="glass-panel" style={{
        maxWidth: '520px',
        width: '100%',
        padding: '32px',
        borderRadius: '20px',
        position: 'relative',
        background: 'linear-gradient(135deg, rgba(15, 23, 42, 0.95), rgba(17, 24, 39, 0.98))',
        border: '1px solid rgba(6, 182, 212, 0.3)',
        boxShadow: '0 20px 50px rgba(0, 0, 0, 0.8), 0 0 30px rgba(6, 182, 212, 0.2)'
      }}>
        {/* Close Button */}
        <button
          onClick={onClose}
          style={{
            position: 'absolute',
            top: '20px',
            right: '20px',
            background: 'rgba(255, 255, 255, 0.08)',
            border: 'none',
            color: 'var(--text-muted)',
            width: '36px',
            height: '36px',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer'
          }}
        >
          <X size={18} />
        </button>

        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <div style={{
            width: '52px',
            height: '52px',
            borderRadius: '16px',
            background: 'linear-gradient(135deg, rgba(6, 182, 212, 0.2), rgba(139, 92, 246, 0.2))',
            border: '1px solid var(--primary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 14px auto'
          }}>
            <QrCode size={26} color="var(--primary)" />
          </div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: '#FFFFFF', marginBottom: '4px' }}>
            Walk-in QR Code Sign
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem' }}>
            {queue?.business?.name || 'Clinic'} • {queue?.service?.name || 'Queue'}
          </p>
        </div>

        {/* QR Code Graphic */}
        <div style={{
          background: '#FFFFFF',
          padding: '20px',
          borderRadius: '16px',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          margin: '0 auto 20px auto',
          maxWidth: '280px',
          boxShadow: '0 0 24px rgba(255, 255, 255, 0.15)'
        }}>
          {qrDataUrl ? (
            <img src={qrDataUrl} alt="Queue QR Code" style={{ width: '100%', height: 'auto', display: 'block' }} />
          ) : (
            <div style={{ height: '240px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#64748B' }}>
              Generating QR Code...
            </div>
          )}
        </div>

        {/* Link input with copy */}
        <div style={{
          background: 'rgba(0, 0, 0, 0.4)',
          border: '1px solid var(--border-subtle)',
          borderRadius: '10px',
          padding: '10px 14px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '10px',
          marginBottom: '20px'
        }}>
          <span style={{
            fontSize: '0.82rem',
            fontFamily: 'var(--font-mono)',
            color: 'var(--text-muted)',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap'
          }}>
            {joinUrl}
          </span>
          <button
            onClick={handleCopyLink}
            style={{
              background: copied ? 'var(--success)' : 'rgba(255, 255, 255, 0.08)',
              border: 'none',
              color: copied ? '#090D16' : '#FFFFFF',
              padding: '6px 12px',
              borderRadius: '6px',
              fontSize: '0.78rem',
              fontWeight: 600,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              flexShrink: 0
            }}
          >
            {copied ? <Check size={14} /> : <Copy size={14} />}
            {copied ? 'Copied!' : 'Copy'}
          </button>
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
          <button
            onClick={handlePrint}
            style={{
              background: 'linear-gradient(135deg, #06B6D4, #0891B2)',
              border: 'none',
              color: '#090D16',
              padding: '12px',
              borderRadius: '10px',
              fontSize: '0.9rem',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px'
            }}
          >
            <Printer size={16} />
            Print Waiting Room Flyer
          </button>

          <a
            href={joinUrl}
            target="_blank"
            rel="noopener noreferrer"
            style={{
              background: 'rgba(255, 255, 255, 0.06)',
              border: '1px solid var(--border-subtle)',
              color: '#FFFFFF',
              padding: '12px',
              borderRadius: '10px',
              fontSize: '0.9rem',
              fontWeight: 600,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              textDecoration: 'none'
            }}
          >
            <ExternalLink size={16} />
            Test Customer Join Link
          </a>
        </div>
      </div>
    </div>
  );
}

