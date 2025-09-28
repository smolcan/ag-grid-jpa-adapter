import {ModuleRegistry} from 'ag-grid-community';
import {
    ColDef, ColumnAutoSizeModule, DateFilterModule, GridReadyEvent,
    IServerSideDatasource, NumberFilterModule, PaginationModule,
    ServerSideRowModelModule, TextFilterModule,
    themeQuartz, ValidationModule, GridApi, EventApiModule, RowGroupingPanelModule
} from 'ag-grid-enterprise';
import React, {useMemo, useState, useCallback, useRef} from 'react';
import {useColorMode} from '@docusaurus/theme-common';
import {AgGridReact} from 'ag-grid-react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';

ModuleRegistry.registerModules([ServerSideRowModelModule, NumberFilterModule, DateFilterModule, TextFilterModule, ValidationModule, ColumnAutoSizeModule, PaginationModule, EventApiModule, RowGroupingPanelModule]);

const PaginationGrid = () => {
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const pagination = true;
    let refetchRowCount = true;
    const gridApiRef = useRef<GridApi | null>(null);
    const { siteConfig } = useDocusaurusContext();
    const { API_URL } = siteConfig.customFields;

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
            headerName: 'Trade Id',
            field: 'tradeId',
            cellDataType: 'number',
            filter: 'agNumberColumnFilter',
        },
        {
            headerName: 'Submitter ID',
            field: 'submitterId',
            cellDataType: 'number',
            filter: 'agNumberColumnFilter',
        },
        {
            headerName: 'Portfolio',
            field: 'portfolio',
            cellDataType: 'text',
            filter: 'agTextColumnFilter',
            enableRowGroup: true,
        },
        {
            headerName: 'Birth Date',
            field: 'birthDate',
            cellDataType: 'dateString',
            filter: 'agDateColumnFilter',
        },
    ] as ColDef[], []);

    const defaultColDef = useMemo(() => ({
        resizable: true,
        filter: true,
        flex: 1,
    } as ColDef), []);

    const serverSideDatasource: IServerSideDatasource = useMemo(() => ({
        getRows: (params) => {
            const dataRequest = fetch(`${API_URL}/docs/pagination/getRows`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(params.request)
            }).then(async response => {
                if (!response.ok) {
                    const errorText = await response.text();
                    throw new Error(errorText || `HTTP error! status: ${response.status}`);
                }
                return response.json();
            });

            if (pagination && refetchRowCount) {
                const countRequest = fetch(`${API_URL}/docs/pagination/countRows`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(params.request)
                }).then(async response => {
                    if (!response.ok) {
                        const errorText = await response.text();
                        throw new Error(errorText || `HTTP error! status: ${response.status}`);
                    }
                    return response.json();
                });

                Promise.all([dataRequest, countRequest])
                    .then(([data, rowCount]) => {
                        setErrorMessage(null);
                        refetchRowCount = false;
                        params.success({
                            ...data,
                            rowCount: rowCount,
                        });
                    })
                    .catch(error => {
                        console.error('Error fetching data:', error);
                        setErrorMessage(error.message || 'Failed to fetch data');
                        params.fail();
                    });
            } else {
                dataRequest
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
        }
    }), []);

    const onGridReady = useCallback((params: GridReadyEvent) => {
        gridApiRef.current = params.api;
        params.api.sizeColumnsToFit();

        // Add event listeners to trigger row count refetch
        params.api.addEventListener('filterChanged', () => refetchRowCount = true);
        params.api.addEventListener('columnRowGroupChanged', () => refetchRowCount = true);
        params.api.addEventListener('columnPivotChanged', () => refetchRowCount = true);
        params.api.addEventListener('columnValueChanged', () => refetchRowCount = true);
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
                    pagination={pagination}
                    rowGroupPanelShow={'always'}
                />
            </div>
        </div>
    );
};

export default PaginationGrid;
