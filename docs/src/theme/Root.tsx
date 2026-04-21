import React from 'react';
import ColdStartNotice from '@site/src/components/ColdStartNotice';

export default function Root({ children }) {
    return (
        <>
            {children}
            <ColdStartNotice />
        </>
    );
}
