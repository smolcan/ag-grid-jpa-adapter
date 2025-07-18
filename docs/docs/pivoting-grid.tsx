import {ModuleRegistry} from 'ag-grid-community';
import {
    ColDef,
    ColumnAutoSizeModule,
    EventApiModule, GridApi, GridReadyEvent, IServerSideDatasource,
    NumberFilterModule, PivotModule, RowGroupingPanelModule,
    ServerSideRowModelModule,
    themeQuartz,
    ValidationModule
} from 'ag-grid-enterprise';
import React, {useCallback, useMemo, useRef, useState} from 'react';
import {useColorMode} from '@docusaurus/theme-common';
import {AgGridReact} from 'ag-grid-react';


ModuleRegistry.registerModules([ServerSideRowModelModule, NumberFilterModule, ValidationModule, ColumnAutoSizeModule, EventApiModule, RowGroupingPanelModule, PivotModule]);

const PivotingGrid = () => {
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const gridApiRef = useRef<GridApi | null>(null);

    const { colorMode } = useColorMode();

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
            headerName: 'Product',
            field: 'product',
            cellDataType: 'text',
            rowGroup: true
        },
        {
            headerName: 'Portfolio',
            field: 'portfolio',
            cellDataType: 'text',
            pivot: true,
        },
        {
            headerName: 'Current Value',
            field: 'currentValue',
            cellDataType: 'number',
            enableValue: true,
            initialAggFunc: 'avg'
        },
        {
            headerName: 'Previous Value',
            field: 'previousValue',
            cellDataType: 'number',
            enableValue: true,
            initialAggFunc: 'avg'
        },
    ] as ColDef[], []);

    const defaultColDef = useMemo(() => ({
        resizable: true,
        filter: true,
        flex: 1,
    } as ColDef), []);

    const serverSideDatasource: IServerSideDatasource = useMemo(() => ({
        getRows: (params) => {
            fetch(
                'https://ag-grid-jpa-adapter-docs-backend.onrender.com/docs/pivoting/getRows',
                {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(params.request)
            })
                .then(async response => {
                    if (!response.ok) {
                        const errorText = await response.text(); // Read plain text from Spring Boot
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
    }), []);

    const onGridReady = useCallback((params: GridReadyEvent) => {
        gridApiRef.current = params.api;
        params.api.sizeColumnsToFit();
    }, []);

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
            <div style={{ height: '500px', width: '100%' }}>
                <AgGridReact
                    columnDefs={columnDefs}
                    defaultColDef={defaultColDef}
                    serverSideDatasource={serverSideDatasource}
                    onGridReady={onGridReady}
                    rowModelType="serverSide"
                    theme={theme}
                    animateRows={true}
                    suppressMenuHide={true}
                    pivotMode={true}
                    rowGroupPanelShow={'always'}
                />
            </div>
        </div>
    );
};

export default PivotingGrid;
