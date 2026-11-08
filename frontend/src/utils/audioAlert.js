// Web Audio API chime synthesizer & Browser notification manager
// 100% self-contained, zero external audio files or CDNs needed

let audioCtx = null;

function getAudioContext() {
  if (!audioCtx) {
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    if (AudioContext) {
      audioCtx = new AudioContext();
    }
  }
  if (audioCtx && audioCtx.state === 'suspended') {
    audioCtx.resume();
  }
  return audioCtx;
}

/**
 * Plays a pleasant two-tone hospital/airport style chime (D5 -> A5)
 * Harmonic envelope with smooth decay
 */
export function playCallChime() {
  try {
    const ctx = getAudioContext();
    if (!ctx) return;

    const now = ctx.currentTime;

    // Tone 1: D5 (587.33 Hz)
    const osc1 = ctx.createOscillator();
    const gain1 = ctx.createGain();
    osc1.type = 'sine';
    osc1.frequency.setValueAtTime(587.33, now);
    gain1.gain.setValueAtTime(0, now);
    gain1.gain.linearRampToValueAtTime(0.25, now + 0.04);
    gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.6);
    osc1.connect(gain1);
    gain1.connect(ctx.destination);
    osc1.start(now);
    osc1.stop(now + 0.6);

    // Tone 2: A5 (880.00 Hz) - starts slightly after tone 1
    const osc2 = ctx.createOscillator();
    const gain2 = ctx.createGain();
    osc2.type = 'sine';
    osc2.frequency.setValueAtTime(880.0, now + 0.22);
    gain2.gain.setValueAtTime(0, now + 0.22);
    gain2.gain.linearRampToValueAtTime(0.3, now + 0.26);
    gain2.gain.exponentialRampToValueAtTime(0.001, now + 1.2);
    osc2.connect(gain2);
    gain2.connect(ctx.destination);
    osc2.start(now + 0.22);
    osc2.stop(now + 1.2);

    // Harmonic richness (soft chime shimmer)
    const osc3 = ctx.createOscillator();
    const gain3 = ctx.createGain();
    osc3.type = 'triangle';
    osc3.frequency.setValueAtTime(1174.66, now + 0.22); // D6
    gain3.gain.setValueAtTime(0, now + 0.22);
    gain3.gain.linearRampToValueAtTime(0.08, now + 0.26);
    gain3.gain.exponentialRampToValueAtTime(0.001, now + 0.9);
    osc3.connect(gain3);
    gain3.connect(ctx.destination);
    osc3.start(now + 0.22);
    osc3.stop(now + 0.9);
  } catch (e) {
    console.warn("Could not play chime", e);
  }
}

/**
 * Request permission for native browser notifications
 */
export async function requestNotificationPermission() {
  if (!('Notification' in window)) return 'unsupported';
  if (Notification.permission === 'granted') return 'granted';
  try {
    const res = await Notification.requestPermission();
    return res;
  } catch (e) {
    return 'denied';
  }
}

/**
 * Send native desktop / lockscreen push notification if permitted
 */
export function sendBrowserNotification(title, options = {}) {
  if (!('Notification' in window)) return null;
  if (Notification.permission !== 'granted') return null;

  try {
    return new Notification(title, {
      icon: '/favicon.ico',
      badge: '/favicon.ico',
      ...options
    });
  } catch (e) {
    console.warn("Could not send notification", e);
    return null;
  }
}

/**
 * Flash browser tab title to alert the user even if muted or backgrounded
 */
let titleInterval = null;
let originalTitle = document.title;

export function flashTabTitle(alertText) {
  if (titleInterval) return;
  originalTitle = document.title;
  let toggle = false;
  titleInterval = setInterval(() => {
    document.title = toggle ? alertText : originalTitle;
    toggle = !toggle;
  }, 1000);
}

export function stopFlashingTitle() {
  if (titleInterval) {
    clearInterval(titleInterval);
    titleInterval = null;
    document.title = originalTitle;
  }
}

