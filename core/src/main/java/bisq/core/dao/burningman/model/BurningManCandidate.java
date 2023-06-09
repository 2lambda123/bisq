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

package bisq.core.dao.burningman.model;

import bisq.core.dao.burningman.BurningManService;

import bisq.common.util.DateUtil;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Contains all relevant data for a burningman candidate (any contributor who has made a compensation request or was
 * a receiver of a genesis output).
 */
@Slf4j
@Getter
@EqualsAndHashCode
public class BurningManCandidate {
    private final Set<CompensationModel> compensationModels = new HashSet<>();
    private long accumulatedCompensationAmount;
    private long accumulatedDecayedCompensationAmount;
    private double compensationShare;           // Share of accumulated decayed compensation amounts in relation to total issued amounts
    protected Optional<String> mostRecentAddress = Optional.empty();

    private final Set<BurnOutputModel> burnOutputModels = new HashSet<>();
    private final Map<Date, Set<BurnOutputModel>> burnOutputModelsByMonth = new HashMap<>();
    private long accumulatedBurnAmount;
    private long accumulatedDecayedBurnAmount;
    // Share of accumulated decayed burn amounts in relation to total burned amounts
    protected double burnAmountShare;
    // Capped burnAmountShare. Cannot be larger than boostedCompensationShare
    protected double cappedBurnAmountShare;
    // The burnAmountShare adjusted in case there are cappedBurnAmountShare.
    // We redistribute the over-burned amounts to the group of not capped candidates.
    protected double adjustedBurnAmountShare;

    public BurningManCandidate() {
    }

    public void addBurnOutputModel(BurnOutputModel burnOutputModel) {
        if (burnOutputModels.contains(burnOutputModel)) {
            return;
        }
        burnOutputModels.add(burnOutputModel);

        Date month = DateUtil.getStartOfMonth(new Date(burnOutputModel.getDate()));
        burnOutputModelsByMonth.putIfAbsent(month, new HashSet<>());
        burnOutputModelsByMonth.get(month).add(burnOutputModel);

        accumulatedDecayedBurnAmount += burnOutputModel.getDecayedAmount();
        accumulatedBurnAmount += burnOutputModel.getAmount();
    }

    public void addCompensationModel(CompensationModel compensationModel) {
        if (compensationModels.contains(compensationModel)) {
            return;
        }

        compensationModels.add(compensationModel);

        accumulatedDecayedCompensationAmount += compensationModel.getDecayedAmount();
        accumulatedCompensationAmount += compensationModel.getAmount();

        mostRecentAddress = compensationModels.stream()
                .max(Comparator.comparing(CompensationModel::getHeight))
                .map(CompensationModel::getAddress);
    }

    public Set<String> getAllAddresses() {
        return compensationModels.stream().map(CompensationModel::getAddress).collect(Collectors.toSet());
    }

    public void calculateShares(double totalDecayedCompensationAmounts, double totalDecayedBurnAmounts) {
        compensationShare = totalDecayedCompensationAmounts > 0 ? accumulatedDecayedCompensationAmount / totalDecayedCompensationAmounts : 0;
        burnAmountShare = totalDecayedBurnAmounts > 0 ? accumulatedDecayedBurnAmount / totalDecayedBurnAmounts : 0;
    }

    public void calculateCappedAndAdjustedShares(double sumAllCappedBurnAmountShares,
                                                 double sumAllNonCappedBurnAmountShares) {
        double maxBoostedCompensationShare = getMaxBoostedCompensationShare();
        adjustedBurnAmountShare = burnAmountShare;
        if (burnAmountShare < maxBoostedCompensationShare) {
            if (sumAllCappedBurnAmountShares == 0) {
                // If no one is capped we do not need to do any adjustment
                cappedBurnAmountShare = burnAmountShare;
            } else {
                // The difference of the cappedBurnAmountShare and burnAmountShare will get redistributed to all
                // non-capped candidates.
                double distributionBase = 1 - sumAllCappedBurnAmountShares;
                if (sumAllNonCappedBurnAmountShares == 0) {
                    // In case we get sumAllNonCappedBurnAmountShares our burnAmountShare is also 0.
                    cappedBurnAmountShare = burnAmountShare;
                } else {
                    double adjustment = distributionBase / sumAllNonCappedBurnAmountShares;
                    adjustedBurnAmountShare = burnAmountShare * adjustment;
                    if (adjustedBurnAmountShare < maxBoostedCompensationShare) {
                        cappedBurnAmountShare = adjustedBurnAmountShare;
                    } else {
                        // We exceeded the cap by the adjustment. This will lead to the legacy BM getting the
                        // difference of the adjusted amount and the maxBoostedCompensationShare.
                        cappedBurnAmountShare = maxBoostedCompensationShare;
                    }
                }
            }
        } else {
            cappedBurnAmountShare = maxBoostedCompensationShare;
        }
    }


    public double getMaxBoostedCompensationShare() {
        return Math.min(BurningManService.MAX_BURN_SHARE, compensationShare * BurningManService.ISSUANCE_BOOST_FACTOR);
    }

    @Override
    public String toString() {
        return "BurningManCandidate{" +
                "\r\n     compensationModels=" + compensationModels +
                ",\r\n     accumulatedCompensationAmount=" + accumulatedCompensationAmount +
                ",\r\n     accumulatedDecayedCompensationAmount=" + accumulatedDecayedCompensationAmount +
                ",\r\n     compensationShare=" + compensationShare +
                ",\r\n     mostRecentAddress=" + mostRecentAddress +
                ",\r\n     burnOutputModels=" + burnOutputModels +
                ",\r\n     accumulatedBurnAmount=" + accumulatedBurnAmount +
                ",\r\n     accumulatedDecayedBurnAmount=" + accumulatedDecayedBurnAmount +
                ",\r\n     burnAmountShare=" + burnAmountShare +
                ",\r\n     cappedBurnAmountShare=" + cappedBurnAmountShare +
                ",\r\n     adjustedBurnAmountShare=" + adjustedBurnAmountShare +
                "\r\n}";
    }
}
