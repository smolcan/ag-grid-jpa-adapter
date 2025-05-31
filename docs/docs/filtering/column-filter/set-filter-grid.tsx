import {ModuleRegistry} from 'ag-grid-community';
import {
    ColDef, ColumnAutoSizeModule, GridReadyEvent,
    IServerSideDatasource,
    ServerSideRowModelModule, SetFilterModule, SetFilterParams,
    themeQuartz, ValidationModule
} from 'ag-grid-enterprise';
import React, {useMemo, useState} from 'react';
import {useColorMode} from '@docusaurus/theme-common';
import {AgGridReact} from 'ag-grid-react';


ModuleRegistry.registerModules([ServerSideRowModelModule, SetFilterModule, ValidationModule, ColumnAutoSizeModule]);

const SetFilterGrid = () => {
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
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
            filter: false,
        },
        {
            headerName: 'Product',
            field: 'product',
            cellDataType: 'text',
            filter: 'agSetColumnFilter',
            filterParams: {
                values: ['Product 1', 'Product 2', 'Product 3', 'Product 4', 'Product 5', 'Product 6', 'Product 7', 'Product 8', 'Product 9', 'Product 10'],
            } as SetFilterParams,
        },
        {
            headerName: 'Portfolio',
            field: 'portfolio',
            cellDataType: 'text',
            filter: 'agSetColumnFilter',
            filterParams: {
                values: ['Portfolio 1', 'portfolio 2', 'Portfolio 3', 'PORTFOLIO 4', 'Portfolio 5', 'Portfolio 6', 'Portfolio 7', 'Portfolio 8', 'Portfolio 9', 'Portfolio 10'],
            } as SetFilterParams,
        },
        {
            headerName: 'Book',
            field: 'book',
            cellDataType: 'text',
            filter: 'agSetColumnFilter',
            filterParams: {
                values: ['Bóok 1', 'Bóók 2', 'Boók 3'],
            } as SetFilterParams,
        },
        {
            headerName: 'Submitter Id',
            field: 'submitterId',
            cellDataType: 'number',
            filter: 'agSetColumnFilter',
            filterParams: {
                values: [10, 20, 30, 40, 50, 60, 70],
            }
        },
        {
            headerName: 'Birth Date',
            field: 'birthDate',
            cellDataType: 'dateString',
            filter: 'agSetColumnFilter',
            filterParams: {
                values: function() {
                    const count = 50;
                    const daysBack = 365;
                    
                    const dates = new Set();
                    while (dates.size < count) {
                        const offset = Math.floor(Math.random() * daysBack);
                        const date = new Date();
                        date.setDate(date.getDate() - offset);
                        const formatted = date.toISOString().split('T')[0]; // Format: YYYY-MM-DD
                        dates.add(formatted);
                    }
                    return Array.from(dates);
                }(),
            }
        },
        {
            headerName: 'Is Sold',
            field: 'isSold',
            cellDataType: 'boolean',
            filter: 'agSetColumnFilter',
            filterParams: {
                values: [true, false, undefined],
            }
        },

    ] as ColDef[], []);

    const defaultColDef = useMemo(() => ({
        resizable: true,
        filter: true,
        flex: 1,
    } as ColDef), []);

    const serverSideDatasource: IServerSideDatasource = useMemo(() => ({
        getRows: (params) => {
            fetch('https://ag-grid-jpa-adapter-docs-backend.onrender.com/docs/filtering/column-filter/set-filter/getRows', {
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
                />
            </div>
        </div>
    );
};

export default SetFilterGrid;