import { ModuleRegistry } from 'ag-grid-community';
import {
    ColDef,
    GridReadyEvent,
    IServerSideDatasource,
    ServerSideRowModelModule,
    themeQuartz,
    IDoesFilterPassParams,
    CustomFilterModule,
    ValidationModule,
    ColumnAutoSizeModule,
} from 'ag-grid-enterprise';
import React, { useMemo, useState, ChangeEvent } from 'react';
import { AgGridReact, useGridFilter } from 'ag-grid-react';
import { useColorMode } from '@docusaurus/theme-common';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';

ModuleRegistry.registerModules([
    ServerSideRowModelModule,
    CustomFilterModule,
    ValidationModule,
    ColumnAutoSizeModule,
]);

type CustomNumberFilterModel = {
    value: string;
}

// Modern Custom Number Filter with auto-apply (no apply button needed)
const CustomNumberFilter = ({ model, onModelChange }) => {
    useGridFilter({
        doesFilterPass: (params: IDoesFilterPassParams) => {
            if (!model) return true;
            const tradeId = params.data.tradeId;
            if (tradeId === null || tradeId === undefined) return false;
            const mod = Number(tradeId) % 2;
            return model === 'Even' ? mod === 0 : mod === 1;
        },
    });

    const handleValueChange = (event: ChangeEvent<HTMLInputElement>) => {
        const newValue = event.target.value;
        const newModel = newValue === 'All' ? null : {
            value: newValue
        } as CustomNumberFilterModel;
        // Automatically apply the filter when selection changes
        onModelChange(newModel);
    };

    return (
        <div style={{ width: '200px', padding: '8px' }}>
            <div style={{ fontWeight: 'bold', marginBottom: '8px' }}>
                Select Filter Type
            </div>
            <label style={{ display: 'block', margin: '8px 0', cursor: 'pointer' }}>
                <input
                    type="radio"
                    name="value"
                    value="All"
                    checked={!model}
                    onChange={handleValueChange}
                    style={{ marginRight: '8px' }}
                />
                All
            </label>
            <label style={{ display: 'block', margin: '8px 0', cursor: 'pointer' }}>
                <input
                    type="radio"
                    name="value"
                    value="Even"
                    checked={model?.value === 'Even'}
                    onChange={handleValueChange}
                    style={{ marginRight: '8px' }}
                />
                Even
            </label>
            <label style={{ display: 'block', margin: '8px 0', cursor: 'pointer' }}>
                <input
                    type="radio"
                    name="value"
                    value="Odd"
                    checked={model?.value === 'Odd'}
                    onChange={handleValueChange}
                    style={{ marginRight: '8px' }}
                />
                Odd
            </label>
        </div>
    );
};

const CustomFilterGrid: React.FC = () => {
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const { colorMode } = useColorMode();
    const { siteConfig } = useDocusaurusContext();
    const { API_URL } = siteConfig.customFields;

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

    const columnDefs = useMemo((): ColDef[] => [
        {
            field: 'tradeId',
            headerName: 'Trade ID',
            sortable: true,
            cellDataType: 'number',
            filter: CustomNumberFilter,
        },
        {
            field: 'submitterDeal.id',
            headerName: 'Submitter Deal Id',
            sortable: true,
            cellDataType: 'number',
            filter: CustomNumberFilter,
        },

    ], []);

    const defaultColDef = useMemo((): ColDef => ({
        resizable: true,
        filter: true,
        flex: 1,
    }), []);

    const serverSideDatasource: IServerSideDatasource = useMemo(() => ({
        getRows: (params) => {
            fetch(`${API_URL}/docs/filtering/column-filter/custom-filter/getRows`, {
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
    }), []);

    const onGridReady = (params: GridReadyEvent): void => {
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
                    rowModelType="serverSide"
                    theme={theme}
                    animateRows={true}
                    suppressMenuHide={true}
                />
            </div>
        </div>
    );
};

export default CustomFilterGrid;