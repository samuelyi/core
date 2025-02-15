import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator';
import { of } from 'rxjs';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { Sidebar } from 'primeng/sidebar';
import { Slider, SliderModule } from 'primeng/slider';

import { DotMessageService } from '@dotcms/data-access';
import { ExperimentSteps } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { getExperimentMock } from '@portlets/dot-experiments/test/mocks';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsConfigurationTrafficAllocationAddComponent } from './dot-experiments-configuration-traffic-allocation-add.component';

const messageServiceMock = new MockDotMessageService({
    Done: 'Done'
});

const EXPERIMENT_MOCK = getExperimentMock(0);

describe('DotExperimentsConfigurationTrafficAllocationAddComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationTrafficAllocationAddComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let sidebar: Sidebar;

    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule, SliderModule, InputTextModule],
        component: DotExperimentsConfigurationTrafficAllocationAddComponent,
        componentProviders: [],
        providers: [
            DotExperimentsConfigurationStore,
            mockProvider(DotExperimentsService),
            mockProvider(MessageService),
            mockProvider(DotHttpErrorManagerService),
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(async () => {
        spectator = createComponent({
            detectChanges: false
        });

        store = spectator.inject(DotExperimentsConfigurationStore);
        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.and.returnValue(of(EXPERIMENT_MOCK));

        store.loadExperiment(EXPERIMENT_MOCK.id);
        store.setSidebarStatus({
            experimentStep: ExperimentSteps.TRAFFIC_LOAD,
            isOpen: true
        });
        spectator.detectChanges();
    });

    it('should load allocation value', () => {
        const slider: Slider = spectator.query(Slider);
        const input: HTMLInputElement = spectator.query(byTestId('traffic-allocation-input'));

        expect(slider.value).toEqual(EXPERIMENT_MOCK.trafficAllocation);
        expect(parseInt(input.value)).toEqual(EXPERIMENT_MOCK.trafficAllocation);
    });

    it('should save form when is valid ', () => {
        spyOn(store, 'setSelectedAllocation');
        const submitButton = spectator.query(
            byTestId('add-trafficAllocation-button')
        ) as HTMLButtonElement;

        expect(submitButton.disabled).toBeFalse();
        expect(submitButton).toContainText('Done');
        expect(spectator.component.form.valid).toBeTrue();

        spectator.click(submitButton);
        expect(store.setSelectedAllocation).toHaveBeenCalledWith({
            trafficAllocation: EXPERIMENT_MOCK.trafficAllocation,
            experimentId: EXPERIMENT_MOCK.id
        });
    });

    it('should set inputs limits', () => {
        const slider: Slider = spectator.query(Slider);

        expect(slider.min).toEqual(1);
        expect(slider.max).toEqual(100);
    });

    it('should close sidebar ', () => {
        spyOn(store, 'closeSidebar');
        sidebar = spectator.query(Sidebar);
        sidebar.hide();

        expect(store.closeSidebar).toHaveBeenCalledTimes(1);
    });
});
