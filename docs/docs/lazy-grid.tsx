import React from 'react';
import { useInView } from 'react-intersection-observer';

const LazyGrid = ({ children }) => {
    const { ref, inView } = useInView({
        triggerOnce: true,
        rootMargin: '100px 0px',
    });

    return (
        <div ref={ref} style={{ minHeight: '450px', marginBottom: '2rem' }}>
            {inView ? children : <div style={{ textAlign: 'center', padding: '2rem' }}>Loading grid...</div>}
        </div>
    );
};

export default LazyGrid;