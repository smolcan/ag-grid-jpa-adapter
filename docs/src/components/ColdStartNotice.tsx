import React, { useEffect, useState } from 'react';

const STORAGE_KEY = 'render-cold-start-notice-dismissed';

const styles: Record<string, React.CSSProperties> = {
    toast: {
        position: 'fixed',
        bottom: '1.5rem',
        right: '1.5rem',
        zIndex: 9999,
        maxWidth: '360px',
        backgroundColor: 'var(--ifm-color-info-contrast-background, #eef9fd)',
        color: 'var(--ifm-color-info-contrast-foreground, #193c47)',
        border: '1px solid var(--ifm-color-info-dark, #4cb3d4)',
        borderLeft: '4px solid var(--ifm-color-info, #54c7ec)',
        borderRadius: '6px',
        padding: '0.85rem 1rem',
        boxShadow: '0 4px 14px rgba(0,0,0,0.15)',
        display: 'flex',
        gap: '0.75rem',
        alignItems: 'flex-start',
        animation: 'slideUp 0.3s ease-out',
    },
    icon: {
        fontSize: '1.1rem',
        flexShrink: 0,
        marginTop: '1px',
    },
    body: {
        flex: 1,
        fontSize: '0.875rem',
        lineHeight: 1.5,
    },
    title: {
        fontWeight: 600,
        marginBottom: '0.2rem',
    },
    close: {
        background: 'none',
        border: 'none',
        cursor: 'pointer',
        fontSize: '1rem',
        lineHeight: 1,
        padding: '0',
        color: 'inherit',
        opacity: 0.6,
        flexShrink: 0,
    },
};

export default function ColdStartNotice() {
    const [visible, setVisible] = useState(false);

    useEffect(() => {
        if (!sessionStorage.getItem(STORAGE_KEY)) {
            setVisible(true);
        }
    }, []);

    const dismiss = () => {
        sessionStorage.setItem(STORAGE_KEY, '1');
        setVisible(false);
    };

    if (!visible) return null;

    return (
        <>
            <style>{`
                @keyframes slideUp {
                    from { opacity: 0; transform: translateY(12px); }
                    to   { opacity: 1; transform: translateY(0); }
                }
            `}</style>
            <div style={styles.toast} role="status" aria-live="polite">
                <span style={styles.icon}>ℹ️</span>
                <div style={styles.body}>
                    <div style={styles.title}>Backend cold start</div>
                    The demo backend runs on a free Render instance and may take a few seconds to wake up.
                    Example grids might be slow to load on first visit.
                </div>
                <button style={styles.close} onClick={dismiss} aria-label="Dismiss">✕</button>
            </div>
        </>
    );
}
