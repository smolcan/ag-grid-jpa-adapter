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

const EagerMasterDetailGrid = () => {

    const { siteConfig } = useDocusaurusContext();
    const { API_URL } = siteConfig.customFields;

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

    // MASTER Columns (Submitter)
    const columnDefs = useMemo(() => [
        {
            field: 'id',
            headerName: 'Submitter ID',
            cellRenderer: "agGroupCellRenderer",
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

    // Master Datasource
    const serverSideDatasource: IServerSideDatasource = useMemo(() => ({
        getRows: (params) => {
            // We assume a specific endpoint configured for Eager loading
            fetch(`${API_URL}/docs/master-detail/eager/getRows`, {
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
                    // Data here ALREADY contains "detailRows" nested array
                    setErrorMessage(null);
                    params.success(data);
                })
                .catch(error => {
                    console.error('Error fetching data:', error);
                    setErrorMessage(error.message || 'Failed to fetch data');
                    params.fail();
                });
        }
    }), [API_URL]);

    // Detail Configuration for EAGER Loading
    const detailCellRendererParams = useMemo(() => {
        return (params: ICellRendererParams): IDetailCellRendererParams => {

            return {
                detailGridOptions: {
                    columnDefs: [
                        { field: 'tradeId', headerName: 'Trade ID' },
                        { field: 'product', headerName: 'Product' },
                        { field: 'portfolio', headerName: 'Portfolio' }
                    ],
                    defaultColDef: { flex: 1 }
                },

                // CRITICAL FOR EAGER LOADING:
                // We do NOT fetch here. We simply pick the data from the master row.
                getDetailRowData: (params: GetDetailRowDataParams) => {
                    // The field name "detailRows" matches .masterDetailRowDataFieldName("detailRows") in Java
                    const nestedDetails = params.data.detailRows;

                    if (nestedDetails) {
                        params.successCallback(nestedDetails);
                    } else {
                        params.successCallback([]);
                    }
                }
            } as IDetailCellRendererParams;
        };
    }, []);

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

export default EagerMasterDetailGrid;