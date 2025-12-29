import React, { useMemo, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { ModuleRegistry } from 'ag-grid-community';
import {
    ClientSideRowModelModule,
    ColDef, ColumnAutoSizeModule, GetDetailRowDataParams,
    GridReadyEvent, ICellRendererParams, IDetailCellRendererParams,
    IServerSideDatasource, MasterDetailModule, ServerSideRowModelModule,
    themeQuartz, ValidationModule
} from 'ag-grid-enterprise';
import { useColorMode } from '@docusaurus/theme-common';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';

// Register AG Grid modules
ModuleRegistry.registerModules([
    ServerSideRowModelModule,
    ClientSideRowModelModule,
    ValidationModule,
    ColumnAutoSizeModule,
    MasterDetailModule
]);

const MasterDetailDynamicRowsGrid = () => {

    const { siteConfig } = useDocusaurusContext();
    const { API_URL } = siteConfig.customFields;

    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const { colorMode } = useColorMode();

    // Theme configuration based on Docusaurus mode
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

    // MASTER Columns (Submitter Entity)
    const columnDefs = useMemo(() => [
        {
            field: 'id',
            headerName: 'Submitter ID',
            cellRenderer: "agGroupCellRenderer", // Important! This allows expansion
            cellDataType: 'number',
        },
        {
            field: 'name',
            headerName: 'Submitter Name',
            cellDataType: 'text'
        },
    ] as ColDef[], []);


    const defaultColDef = useMemo(() => ({
        resizable: true,
        filter: false,
        flex: 1,
    } as ColDef), []);

    // Master Datasource (Using the Dynamic Endpoint)
    const serverSideDatasource: IServerSideDatasource = useMemo(() => ({
        getRows: (params) => {
            fetch(`${API_URL}/docs/master-detail/dynamic/getRows`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(params.request)
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
                    params.success(data.data);
                })
                .catch(error => {
                    console.error('Error fetching data:', error);
                    setErrorMessage(error.message || 'Failed to fetch data');
                    params.fail();
                });
        }
    }), [API_URL]);

    // Detail Configuration with DYNAMIC COLUMNS
    const detailCellRendererParams = useMemo(() => {
        return (params: ICellRendererParams): IDetailCellRendererParams => {

            // Logic matching Backend: 
            // If Submitter ID is even -> Show Product
            // If Submitter ID is odd  -> Show Portfolio
            const submitterId = params.data.id;
            let detailColDefs: ColDef[];

            if (submitterId % 2 === 0) {
                detailColDefs = [
                    { headerName: 'Trade ID', field: 'tradeId', cellDataType: 'number' },
                    { headerName: 'Product', field: 'product', cellDataType: 'text' }
                ];
            } else {
                detailColDefs = [
                    { headerName: 'Trade ID', field: 'tradeId', cellDataType: 'number' },
                    { headerName: 'Portfolio', field: 'portfolio', cellDataType: 'text' }
                ];
            }

            return {
                // Configure the Detail Grid with dynamic columns
                detailGridOptions: {
                    columnDefs: detailColDefs,
                    defaultColDef: {
                        flex: 1,
                    }
                },
                // API Call for Detail Rows (Using dynamic endpoint)
                getDetailRowData: (params: GetDetailRowDataParams) => {
                    // params.data contains the Master Row (Submitter)
                    fetch(`${API_URL}/docs/master-detail/dynamic/getDetailRowData`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify(params.data)
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
                            params.successCallback(data.data);
                        })
                        .catch(error => {
                            console.error('Error fetching detail data:', error);
                            setErrorMessage(error.message || 'Failed to fetch detail data');
                        });
                }
            } as IDetailCellRendererParams;
        };
    }, [API_URL]);

    const onGridReady = (params: GridReadyEvent) => {
        params.api.sizeColumnsToFit();
    };

    return (
        <div style={{
            backgroundColor: colorMode === 'dark' ? '#1a1c1d' : '#ffffff',
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

                    // Master/Detail Specific Props
                    masterDetail={true}
                    detailCellRendererParams={detailCellRendererParams}

                    rowModelType={"serverSide"}
                    theme={theme}
                    animateRows={true}
                    suppressMenuHide={true}
                />
            </div>
        </div>
    );
};

export default MasterDetailDynamicRowsGrid;