import QRCode from 'qrcode';

/**
 * Generates an SVG string representation of a QR Code for a given URL or text.
 */
export async function generateQRCodeSVG(text, options = {}) {
  try {
    return await QRCode.toString(text, {
      type: 'svg',
      margin: 2,
      errorCorrectionLevel: 'M',
      color: {
        dark: options.darkColor || '#090D16',
        light: options.lightColor || '#FFFFFF'
      },
      width: options.width || 260
    });
  } catch (err) {
    console.error('Failed to generate QR Code SVG', err);
    return null;
  }
}

/**
 * Generates a PNG Data URL of a QR Code for displaying in <img> or downloading.
 */
export async function generateQRCodeDataURL(text, options = {}) {
  try {
    return await QRCode.toDataURL(text, {
      margin: 2,
      errorCorrectionLevel: 'M',
      color: {
        dark: options.darkColor || '#090D16',
        light: options.lightColor || '#FFFFFF'
      },
      width: options.width || 320
    });
  } catch (err) {
    console.error('Failed to generate QR Code Data URL', err);
    return null;
  }
}

