/**
 * Application state context for SignalR data.
 * Per WindowsAppStructureSpec §2B(1)(d)(iv).
 */

import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import signalRService from '../services/signalr/SignalRService';
import type {
    UnitCollectionEntity,
    UnitEntity,
    LessonEntity,
    MaterialEntity,
    InternalCreateUnitCollectionDto,
    InternalCreateUnitDto,
    InternalCreateLessonDto,
    InternalCreateMaterialDto,
} from '../models';

interface AppState {
    isConnected: boolean;
    isLoading: boolean;
    error: string | null;
    unitCollections: UnitCollectionEntity[];
    units: UnitEntity[];
    lessons: LessonEntity[];
    materials: MaterialEntity[];
}

interface AppContextValue extends AppState {
    // CRUD operations
    createUnitCollection: (dto: InternalCreateUnitCollectionDto) => Promise<UnitCollectionEntity>;
    updateUnitCollection: (entity: UnitCollectionEntity) => Promise<void>;
    deleteUnitCollection: (id: string) => Promise<void>;

    createUnit: (dto: InternalCreateUnitDto) => Promise<UnitEntity>;
    updateUnit: (entity: UnitEntity) => Promise<void>;
    deleteUnit: (id: string) => Promise<void>;

    createLesson: (dto: InternalCreateLessonDto) => Promise<LessonEntity>;
    updateLesson: (entity: LessonEntity) => Promise<void>;
    deleteLesson: (id: string) => Promise<void>;

    createMaterial: (dto: InternalCreateMaterialDto) => Promise<MaterialEntity>;
    updateMaterial: (entity: MaterialEntity) => Promise<void>;
    deleteMaterial: (id: string) => Promise<void>;

    // Helpers
    getUnitsForCollection: (collectionId: string) => UnitEntity[];
    getLessonsForUnit: (unitId: string) => LessonEntity[];
    getMaterialsForLesson: (lessonId: string) => MaterialEntity[];

    refreshData: () => Promise<void>;
}

const AppContext = createContext<AppContextValue | null>(null);

export const useAppContext = () => {
    const context = useContext(AppContext);
    if (!context) {
        throw new Error('useAppContext must be used within AppProvider');
    }
    return context;
};

interface AppProviderProps {
    children: ReactNode;
}

export const AppProvider: React.FC<AppProviderProps> = ({ children }) => {
    const [state, setState] = useState<AppState>({
        isConnected: false,
        isLoading: true,
        error: null,
        unitCollections: [],
        units: [],
        lessons: [],
        materials: [],
    });

    const fetchAllData = useCallback(async () => {
        try {
            setState(prev => ({ ...prev, isLoading: true, error: null }));

            const [collections, units, lessons, materials] = await Promise.all([
                signalRService.getAllUnitCollections(),
                signalRService.getAllUnits(),
                signalRService.getAllLessons(),
                signalRService.getAllMaterials(),
            ]);

            setState(prev => ({
                ...prev,
                isLoading: false,
                unitCollections: collections,
                units,
                lessons,
                materials,
            }));
        } catch (err) {
            console.error('Failed to fetch data:', err);
            setState(prev => ({
                ...prev,
                isLoading: false,
                error: 'Failed to load data from server.',
            }));
        }
    }, []);

    useEffect(() => {
        const init = async () => {
            await signalRService.startConnection();
            setState(prev => ({ ...prev, isConnected: true }));
            await fetchAllData();
        };
        init();
        return () => signalRService.stopConnection();
    }, [fetchAllData]);

    // CRUD operations
    const createUnitCollection = async (dto: InternalCreateUnitCollectionDto) => {
        const created = await signalRService.createUnitCollection(dto);
        setState(prev => ({ ...prev, unitCollections: [...prev.unitCollections, created] }));
        return created;
    };

    const updateUnitCollection = async (entity: UnitCollectionEntity) => {
        await signalRService.updateUnitCollection(entity);
        setState(prev => ({
            ...prev,
            unitCollections: prev.unitCollections.map(c => c.id === entity.id ? entity : c),
        }));
    };

    const deleteUnitCollection = async (id: string) => {
        await signalRService.deleteUnitCollection(id);
        setState(prev => ({
            ...prev,
            unitCollections: prev.unitCollections.filter(c => c.id !== id),
            units: prev.units.filter(u => u.unitCollectionId !== id),
        }));
    };

    const createUnit = async (dto: InternalCreateUnitDto) => {
        const created = await signalRService.createUnit(dto);
        setState(prev => ({ ...prev, units: [...prev.units, created] }));
        return created;
    };

    const updateUnit = async (entity: UnitEntity) => {
        await signalRService.updateUnit(entity);
        setState(prev => ({
            ...prev,
            units: prev.units.map(u => u.id === entity.id ? entity : u),
        }));
    };

    const deleteUnit = async (id: string) => {
        await signalRService.deleteUnit(id);
        setState(prev => ({
            ...prev,
            units: prev.units.filter(u => u.id !== id),
            lessons: prev.lessons.filter(l => l.unitId !== id),
        }));
    };

    const createLesson = async (dto: InternalCreateLessonDto) => {
        const created = await signalRService.createLesson(dto);
        setState(prev => ({ ...prev, lessons: [...prev.lessons, created] }));
        return created;
    };

    const updateLesson = async (entity: LessonEntity) => {
        await signalRService.updateLesson(entity);
        setState(prev => ({
            ...prev,
            lessons: prev.lessons.map(l => l.id === entity.id ? entity : l),
        }));
    };

    const deleteLesson = async (id: string) => {
        await signalRService.deleteLesson(id);
        setState(prev => ({
            ...prev,
            lessons: prev.lessons.filter(l => l.id !== id),
            materials: prev.materials.filter(m => m.lessonId !== id),
        }));
    };

    const createMaterial = async (dto: InternalCreateMaterialDto) => {
        const created = await signalRService.createMaterial(dto);
        setState(prev => ({ ...prev, materials: [...prev.materials, created] }));
        return created;
    };

    const updateMaterial = async (entity: MaterialEntity) => {
        await signalRService.updateMaterial(entity);
        setState(prev => ({
            ...prev,
            materials: prev.materials.map(m => m.id === entity.id ? entity : m),
        }));
    };

    const deleteMaterial = async (id: string) => {
        await signalRService.deleteMaterial(id);
        setState(prev => ({
            ...prev,
            materials: prev.materials.filter(m => m.id !== id),
        }));
    };

    // Helpers
    const getUnitsForCollection = (collectionId: string) =>
        state.units.filter(u => u.unitCollectionId === collectionId);

    const getLessonsForUnit = (unitId: string) =>
        state.lessons.filter(l => l.unitId === unitId);

    const getMaterialsForLesson = (lessonId: string) =>
        state.materials.filter(m => m.lessonId === lessonId);

    const value: AppContextValue = {
        ...state,
        createUnitCollection,
        updateUnitCollection,
        deleteUnitCollection,
        createUnit,
        updateUnit,
        deleteUnit,
        createLesson,
        updateLesson,
        deleteLesson,
        createMaterial,
        updateMaterial,
        deleteMaterial,
        getUnitsForCollection,
        getLessonsForUnit,
        getMaterialsForLesson,
        refreshData: fetchAllData,
    };

    return (
        <AppContext.Provider value={value}>
            {children}
        </AppContext.Provider>
    );
};
