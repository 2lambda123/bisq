/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.dao.burnbsq;

import bisq.desktop.Navigation;
import bisq.desktop.common.view.ActivatableView;
import bisq.desktop.common.view.CachingViewLoader;
import bisq.desktop.common.view.FxmlView;
import bisq.desktop.common.view.View;
import bisq.desktop.common.view.ViewLoader;
import bisq.desktop.common.view.ViewPath;
import bisq.desktop.components.MenuItem;
import bisq.desktop.main.MainView;
import bisq.desktop.main.dao.DaoView;
import bisq.desktop.main.dao.burnbsq.assetfee.AssetFeeView;
import bisq.desktop.main.dao.burnbsq.burningman.BurningManView;
import bisq.desktop.main.dao.burnbsq.proofofburn.ProofOfBurnView;

import bisq.core.locale.Res;

import javax.inject.Inject;

import javafx.fxml.FXML;

import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.List;

@FxmlView
public class BurnBsqView extends ActivatableView<AnchorPane, Void> {

    private final ViewLoader viewLoader;
    private final Navigation navigation;

    private MenuItem proofOfBurn, burningMan, assetFee;
    private Navigation.Listener listener;

    @FXML
    private VBox leftVBox;
    @FXML
    private AnchorPane content;

    private Class<? extends View> selectedViewClass;
    private ToggleGroup toggleGroup;

    @Inject
    private BurnBsqView(CachingViewLoader viewLoader, Navigation navigation) {
        this.viewLoader = viewLoader;
        this.navigation = navigation;
    }

    @Override
    public void initialize() {
        listener = (viewPath, data) -> {
            if (viewPath.size() != 4 || viewPath.indexOf(BurnBsqView.class) != 2)
                return;

            selectedViewClass = viewPath.tip();
            loadView(selectedViewClass);
        };

        toggleGroup = new ToggleGroup();
        List<Class<? extends View>> baseNavPath = Arrays.asList(MainView.class, DaoView.class, BurnBsqView.class);
        proofOfBurn = new MenuItem(navigation, toggleGroup, Res.get("dao.burnBsq.menuItem.proofOfBurn"),
                ProofOfBurnView.class, baseNavPath);
        burningMan = new MenuItem(navigation, toggleGroup, Res.get("dao.burnBsq.menuItem.burningMan"),
                BurningManView.class, baseNavPath);
        assetFee = new MenuItem(navigation, toggleGroup, Res.get("dao.burnBsq.menuItem.assetFee"),
                AssetFeeView.class, baseNavPath);
        leftVBox.getChildren().addAll(burningMan, proofOfBurn, assetFee);
    }

    @Override
    protected void activate() {
        proofOfBurn.activate();
        burningMan.activate();
        assetFee.activate();

        navigation.addListener(listener);
        ViewPath viewPath = navigation.getCurrentPath();
        if (viewPath.size() == 3 && viewPath.indexOf(BurnBsqView.class) == 2 ||
                viewPath.size() == 2 && viewPath.indexOf(DaoView.class) == 1) {
            if (selectedViewClass == null)
                selectedViewClass = BurningManView.class;

            loadView(selectedViewClass);

        } else if (viewPath.size() == 4 && viewPath.indexOf(BurnBsqView.class) == 2) {
            selectedViewClass = viewPath.get(3);
            loadView(selectedViewClass);
        }
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void deactivate() {
        navigation.removeListener(listener);

        proofOfBurn.deactivate();
        burningMan.deactivate();
        assetFee.deactivate();
    }

    private void loadView(Class<? extends View> viewClass) {
        View view = viewLoader.load(viewClass);
        content.getChildren().setAll(view.getRoot());

        if (view instanceof ProofOfBurnView) toggleGroup.selectToggle(proofOfBurn);
        else if (view instanceof BurningManView) toggleGroup.selectToggle(burningMan);
        else if (view instanceof AssetFeeView) toggleGroup.selectToggle(assetFee);
    }
}
