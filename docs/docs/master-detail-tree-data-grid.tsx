import React, { useMemo, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { ModuleRegistry } from 'ag-grid-community';
import {
    ClientSideRowModelModule,
    ColDef, ColumnAutoSizeModule, GetDetailRowDataParams,
    GridReadyEvent, ICellRendererParams, IDetailCellRendererParams,
    IServerSideDatasource, MasterDetailModule, ServerSideRowModelModule,
    themeQuartz, ValidationModule, RowGroupingModule, TreeDataModule
} from 'ag-grid-enterprise';
import { useColorMode } from '@docusaurus/theme-common';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';

// Register AG Grid modules including TreeDataModule
ModuleRegistry.registerModules([
    ServerSideRowModelModule,
    ClientSideRowModelModule,
    ValidationModule,
    ColumnAutoSizeModule,
    MasterDetailModule,
    RowGroupingModule,
    TreeDataModule
]);

const MasterDetailTreeDataGrid = () => {

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

    // Regular Columns
    const columnDefs = useMemo(() => [
        // Note: 'tradeId' is used in autoGroupColumnDef, so we don't need it here explicitly
        {
            field: 'product',
            headerName: 'Product',
            cellDataType: 'text'
        },
        {
            field: 'portfolio',
            headerName: 'Portfolio',
            cellDataType: 'text'
        },
        { 
            field: 'submitter.id', 
            headerName: 'Submitter ID',
            cellDataType: 'number',
        },
    ] as ColDef[], []);

    // Tree Data configuration - This column holds the hierarchy expander
    const autoGroupColumnDef = useMemo<ColDef>(() => {
        return {
            field: 'tradeId', // Display Trade ID in the tree column
            headerName: 'Trade Hierarchy',
            cellRendererParams: {
                suppressCount: true, // Optional: hide child count
            },
            flex: 1
        };
    }, []);

    const defaultColDef = useMemo(() => ({
        resizable: true,
        filter: false,
        flex: 1,
    } as ColDef), []);

    // Datasource
    const serverSideDatasource: IServerSideDatasource = useMemo(() => ({
        getRows: (params) => {
            // Pointing to a hypothetical endpoint for this combined example
            fetch(`${API_URL}/docs/master-detail/tree/getRows`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
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
                    params.success(data);
                })
                .catch(error => {
                    console.error('Error fetching data:', error);
                    setErrorMessage(error.message || 'Failed to fetch data');
                    params.fail();
                });
        }
    }), [API_URL]);

    // Detail Grid Configuration
    const detailCellRendererParams = useMemo(() => {
        return (params: ICellRendererParams): IDetailCellRendererParams => {
            return {
                detailGridOptions: {
                    columnDefs: [
                        { field: 'tradeId', headerName: 'Detail ID' },
                        { field: 'product', headerName: 'Detail Product' },
                        { field: 'portfolio', headerName: 'Detail Portfolio' },
                        { field: 'submitter.id', headerName: 'Submitter ID' },
                    ],
                    defaultColDef: { flex: 1 }
                },
                getDetailRowData: (params: GetDetailRowDataParams) => {
                    fetch(`${API_URL}/docs/master-detail/tree/getDetailRowData`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(params.data)
                    })
                        .then(r => r.json())
                        .then(data => params.successCallback(data))
                        .catch(err => console.error(err));
                }
            } as IDetailCellRendererParams;
        };
    }, [API_URL]);

    // Callbacks for Tree Data
    const isServerSideGroup = (dataItem: any) => {
        // Must match .isServerSideGroupFieldName("hasChildren") in backend
        return dataItem.hasChildren;
    };

    const getServerSideGroupKey = (dataItem: any) => {
        // Must match .primaryFieldName("tradeId") in backend
        return dataItem.tradeId;
    };

    // Callback for Master/Detail
    const isRowMaster = (dataItem: any) => {
        // Logic to determine if a row has a detail grid. 
        // In this example, we allow any row to have details.
        return !dataItem.hasChildren;
    };

    const onGridReady = (params: GridReadyEvent) => {
        params.api.sizeColumnsToFit();
    };

    return (
        <div style={{
            backgroundColor: colorMode === 'dark' ? '#1a1c1d' : '#ffffff',
            marginBottom: '1rem',
            borderRadius: '8px',
            padding: '1rem'
        }}>
            {errorMessage && (
                <div style={{ color: 'red', marginBottom: '1rem' }}>{errorMessage}</div>
            )}
            <div style={{ height: '500px', width: '100%' }}>
                <AgGridReact
                    columnDefs={columnDefs}
                    defaultColDef={defaultColDef}
                    autoGroupColumnDef={autoGroupColumnDef} // Required for Tree Data

                    serverSideDatasource={serverSideDatasource}
                    rowModelType={"serverSide"}

                    // Tree Data Config
                    treeData={true}
                    isServerSideGroup={isServerSideGroup}
                    getServerSideGroupKey={getServerSideGroupKey}

                    // Master/Detail Config
                    masterDetail={true}
                    isRowMaster={isRowMaster}
                    detailCellRendererParams={detailCellRendererParams}

                    onGridReady={onGridReady}
                    theme={theme}
                    animateRows={true}
                />
            </div>
        </div>
    );
};

export default MasterDetailTreeDataGrid;