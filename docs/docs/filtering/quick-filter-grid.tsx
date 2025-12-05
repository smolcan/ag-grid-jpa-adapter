import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import { AgGridReact } from 'ag-grid-react';
import { ModuleRegistry } from 'ag-grid-community';
import {
    ColDef, ColumnAutoSizeModule,
    GridReadyEvent,
    IServerSideDatasource, NumberFilterModule,
    ServerSideRowModelModule, TextFilterModule,
    themeQuartz, ValidationModule
} from 'ag-grid-enterprise';
import { useColorMode } from '@docusaurus/theme-common';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import {useDebounce} from 'use-debounce';

// Register the required modules
ModuleRegistry.registerModules([ServerSideRowModelModule, ValidationModule, ColumnAutoSizeModule, NumberFilterModule, TextFilterModule]);

const QuickFilterGrid = () => {
    const [quickFilterValue, setQuickFilterValue] = useState('');
    const [debouncedQuickFilter] = useDebounce(quickFilterValue, 300);

    const filterRef = useRef('');

    const { siteConfig } = useDocusaurusContext();
    const { API_URL } = siteConfig.customFields;

    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const { colorMode } = useColorMode();
    const gridRef = useRef<AgGridReact>(null);

    
    useEffect(() => {
        filterRef.current = debouncedQuickFilter;
        if (gridRef?.current?.api) {
            gridRef.current.api.onFilterChanged();
        }
    }, [debouncedQuickFilter]);

    const onFilterTextBoxChanged = useCallback(() => {
        const value = (document.getElementById("filter-text-box") as HTMLInputElement).value;
        setQuickFilterValue(value)
    }, []);

    const theme = useMemo(() =>
        themeQuartz.withParams({
            backgroundColor: colorMode === 'dark' ? "#1f2836" : "#ffffff",
            browserColorScheme: colorMode,
            chromeBackgroundColor: {
                ref: "foregroundColor",
                mix: 0.07,
                onto: "backgroundColor"
            },
            foregroundColor: colorMode === 'dark' ? "#FFF" : "#000",
            headerFontSize: 14
        }), [colorMode]);

    const columnDefs = useMemo(() => [
        {
            headerName: 'Trade ID',
            field: 'tradeId',
            cellDataType: 'number',
            filter: 'agNumberColumnFilter',
        },
        {
            headerName: 'Submitter name',
            field: 'submitter.name',
            cellDataType: 'text',
            filter: 'agTextColumnFilter',
        },
        {
            headerName: 'Product',
            field: 'product',
            cellDataType: 'text',
            filter: 'agTextColumnFilter',
        },
        {
            headerName: 'Portfolio',
            field: 'portfolio',
            cellDataType: 'text',
            filter: 'agTextColumnFilter',
        },
        {
            headerName: 'Book',
            field: 'book',
            cellDataType: 'text',
            filter: 'agTextColumnFilter',
        },
        {
            headerName: 'Deal type',
            field: 'dealType',
            cellDataType: 'text',
            filter: 'agTextColumnFilter',
        },
        {
            headerName: 'Bid type',
            field: 'bidType',
            cellDataType: 'text',
            filter: 'agTextColumnFilter',
        }
    ] as ColDef[], []);

    const defaultColDef = useMemo(() => ({
        resizable: true,
        filter: false,
        flex: 1,
    } as ColDef), []);

    // 3. Read from filterRef.current inside getRows
    // We can keep the dependency array empty [] because refs are mutable
    const serverSideDatasource: IServerSideDatasource = useMemo(() => ({
        getRows: (params) => {
            fetch(`${API_URL}/docs/filtering/quick-filter/getRows`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    ...params.request,
                    quickFilter: filterRef.current,
                })
            })
                .then(async response => {
                    if (!response.ok) {
                        const errorText = await response.text();
                        throw new Error(errorText || `HTTP error! status: ${response.status}`);
                    }
                    return response.json();
                })
                .then(data => {
                    setErrorMessage(null);
                    params.success(data);
                })
                .catch(error => {
                    console.error('Error fetching data:', error);
                    setErrorMessage(error.message || 'Failed to fetch data');
                    params.fail();
                });
        }
    }), [API_URL]); // Only recreate if URL changes

    const onGridReady = (params: GridReadyEvent) => {
        params.api.sizeColumnsToFit();
    };

    return (
        <div style={{
            backgroundColor: colorMode == 'dark' ? '#1a1c1d' : '#ffffff',
            marginBottom: '1rem',
            borderRadius: '8px',
            fontFamily: 'system-ui, -apple-system, sans-serif',
            padding: '1rem'
        }}>
            {errorMessage && (
                <div style={{
                    backgroundColor: '#ff4d4f',
                    color: '#fff',
                    display: 'inline-block',
                    padding: '0.5rem 1rem',
                    borderRadius: '20px',
                    fontSize: '0.875rem',
                    marginBottom: '1rem',
                    fontWeight: 500
                }}>
                    {errorMessage}
                </div>
            )}
            <div className="example-header" style={{marginBottom: '10px'}}>
                <span style={{marginRight: '10px'}}>Quick Filter:</span>
                <input
                    type="text"
                    id="filter-text-box"
                    placeholder="Filter..."
                    onInput={onFilterTextBoxChanged}
                    style={{padding: '5px', borderRadius: '4px', border: '1px solid #ccc'}}
                />
            </div>
            <div style={{ height: '500px', width: '100%' }}>
                <AgGridReact
                    ref={gridRef}
                    columnDefs={columnDefs}
                    defaultColDef={defaultColDef}
                    serverSideDatasource={serverSideDatasource}
                    onGridReady={onGridReady}
                    rowModelType="serverSide"
                    theme={theme}
                    animateRows={true}
                    suppressMenuHide={true}
                />
            </div>
        </div>
    );
};

export default QuickFilterGrid;