package cn.edu.ruc.realtime.generator;

import com.google.common.collect.AbstractIterator;
import io.airlift.tpch.CustomerGenerator;
import io.airlift.tpch.Distributions;
import io.airlift.tpch.GenerateUtils;
import io.airlift.tpch.PartGenerator;
import io.airlift.tpch.RandomBoundedInt;
import io.airlift.tpch.RandomBoundedLong;
import io.airlift.tpch.RandomString;
import io.airlift.tpch.RandomText;
import io.airlift.tpch.SupplierGenerator;
import io.airlift.tpch.TextPool;

import java.util.Iterator;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.airlift.tpch.GenerateUtils.MIN_GENERATE_DATE;
import static io.airlift.tpch.GenerateUtils.TOTAL_DATE_RANGE;
import static io.airlift.tpch.GenerateUtils.calculateRowCount;
import static io.airlift.tpch.GenerateUtils.calculateStartIndex;

/**
 * RealTimeAnalysis
 *
 * @author guodong
 */
public class LineorderGenerator
    implements Iterable<Lineorder>
{
    private static final int QUANTITY_MIN = 1;
    private static final int QUANTITY_MAX = 50;
    private static final int TAX_MIN = 0;
    private static final int TAX_MAX = 8;
    private static final int DISCOUNT_MIN = 0;
    private static final int DISCOUNT_MAX = 10;
    private static final int PART_KEY_MIN = 1;

    private static final int SHIP_DATE_MIN = 1;
    private static final int SHIP_DATE_MAX = 121;
    private static final int COMMIT_DATE_MIN = 30;
    private static final int COMMIT_DATE_MAX = 90;
    private static final int RECEIPT_DATE_MIN = 1;
    private static final int RECEIPT_DATE_MAX = 30;

    private static final int SCALE_BASE = 1_500_000;

    // portion with have no orders
    private static final int CUSTOMER_MORTALITY = 3;

    private static final int ORDER_DATE_MIN = MIN_GENERATE_DATE;
    private static final int ITEM_SHIP_DAYS = SHIP_DATE_MAX + RECEIPT_DATE_MAX;
    private static final int ORDER_DATE_MAX = ORDER_DATE_MIN + (TOTAL_DATE_RANGE - ITEM_SHIP_DAYS - 1);
    private static final int CLERK_SCALE_BASE = 1000;

    private static final int LINE_COUNT_MIN = 1;
    static final int LINE_COUNT_MAX = 7;

    private static final int ORDER_COMMENT_AVERAGE_LENGTH = 49;
    private static final int LINEITEM_COMMENT_AVERAGE_LENGTH = 27;

    private static final int ORDER_KEY_SPARSE_BITS = 2;
    private static final int ORDER_KEY_SPARSE_KEEP = 3;

    private final double scaleFactor;
    private final int part;
    private final int partCount;

    private final Distributions distributions;
    private final TextPool textPool;

    public LineorderGenerator(double scaleFactor, int part, int partCount)
    {
        this(scaleFactor, part, partCount, Distributions.getDefaultDistributions(), TextPool.getDefaultTestPool());
    }

    public LineorderGenerator(double scaleFactor, int part, int partCount, Distributions distributions, TextPool textPool)
    {
        checkArgument(scaleFactor > 0, "scaleFactor must be greater than 0");
        checkArgument(part >= 1, "part must be at least 1");
        checkArgument(part <= partCount, "part must be less than or equal to part count");

        this.scaleFactor = scaleFactor;
        this.part = part;
        this.partCount = partCount;

        this.distributions = checkNotNull(distributions, "distributions is null");
        this.textPool = checkNotNull(textPool, "textPool is null");
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<Lineorder> iterator()
    {
        return new LineorderGeneratorIterator(
                distributions,
                textPool,
                scaleFactor,
                calculateStartIndex(SCALE_BASE, scaleFactor, part, partCount),
                calculateRowCount(SCALE_BASE, scaleFactor, part, partCount));
    }

    private static class LineorderGeneratorIterator
            extends AbstractIterator<Lineorder>
    {
        private final RandomBoundedInt orderDateRandom = createOrderDateRandom();
        private final RandomBoundedInt lineCountRandom = createLineCountRandom();

        private final RandomBoundedLong customerKeyRandom;
        private final RandomString orderPriorityRandom;
        private final RandomBoundedInt clerkRandom;

        private final RandomText orderCommentRandom;
        private final RandomText lineitemCommentRandom;

        private final RandomBoundedInt quantityRandom = createQuantityRandom();
        private final RandomBoundedInt discountRandom = createDiscountRandom();
        private final RandomBoundedInt taxRandom = createTaxRandom();

        private final RandomBoundedLong linePartKeyRandom;

        private final RandomBoundedInt supplierNumberRandom = new RandomBoundedInt(2095021727, 0, 3, LINE_COUNT_MAX);

        private final RandomBoundedInt shipDateRandom = createShipDateRandom();
        private final RandomBoundedInt commitDateRandom = new RandomBoundedInt(904914315, COMMIT_DATE_MIN, COMMIT_DATE_MAX, LINE_COUNT_MAX);
        private final RandomBoundedInt receiptDateRandom = new RandomBoundedInt(373135028, RECEIPT_DATE_MIN, RECEIPT_DATE_MAX, LINE_COUNT_MAX);

        private final RandomString returnedFlagRandom;
        private final RandomString shipInstructionsRandom;
        private final RandomString shipModeRandom;

        private final RandomBoundedLong totalPriceRandom = new RandomBoundedLong(839213222, true, 100L, 10000000L, LINE_COUNT_MAX);

        private final double scaleFactor;
        private final long startIndex;
        private final long rowCount;

        private final long maxCustomerKey;

        private long customerKey;

        private long index;
        private int orderDate;
        private int lineCount;
        private int lineNumber;

        private LineorderGeneratorIterator(Distributions distributions, TextPool textPool, double scaleFactor, long startIndex, long rowCount)
        {
            this.scaleFactor = scaleFactor;
            this.startIndex = startIndex;
            this.rowCount = rowCount;

            maxCustomerKey = (long) (CustomerGenerator.SCALE_BASE * scaleFactor);

            customerKeyRandom = new RandomBoundedLong(851767375, scaleFactor >= 30000, 1, maxCustomerKey);
            orderPriorityRandom = new RandomString(591449447, distributions.getOrderPriorities(), LINE_COUNT_MAX);
            clerkRandom = new RandomBoundedInt(1171034773, 1, Math.max((int) (scaleFactor * CLERK_SCALE_BASE), CLERK_SCALE_BASE), LINE_COUNT_MAX);
            orderCommentRandom = new RandomText(276090261, textPool, ORDER_COMMENT_AVERAGE_LENGTH, LINE_COUNT_MAX);
            lineitemCommentRandom = new RandomText(1095462486, textPool, LINEITEM_COMMENT_AVERAGE_LENGTH, LINE_COUNT_MAX);
            returnedFlagRandom = new RandomString(717419739, distributions.getReturnFlags(), LINE_COUNT_MAX);
            shipInstructionsRandom = new RandomString(1371272478, distributions.getShipInstructions(), LINE_COUNT_MAX);
            shipModeRandom = new RandomString(675466456, distributions.getShipModes(), LINE_COUNT_MAX);

            orderDateRandom.advanceRows(startIndex);
            lineCountRandom.advanceRows(startIndex);

            customerKeyRandom.advanceRows(startIndex);
            orderPriorityRandom.advanceRows(startIndex);
            clerkRandom.advanceRows(startIndex);

            orderCommentRandom.advanceRows(startIndex);
            lineitemCommentRandom.advanceRows(startIndex);

            quantityRandom.advanceRows(startIndex);
            discountRandom.advanceRows(startIndex);
            taxRandom.advanceRows(startIndex);

            linePartKeyRandom = createPartKeyRandom(scaleFactor);
            linePartKeyRandom.advanceRows(startIndex);

            supplierNumberRandom.advanceRows(startIndex);

            shipDateRandom.advanceRows(startIndex);
            commitDateRandom.advanceRows(startIndex);
            receiptDateRandom.advanceRows(startIndex);

            returnedFlagRandom.advanceRows(startIndex);
            shipInstructionsRandom.advanceRows(startIndex);
            shipModeRandom.advanceRows(startIndex);

            totalPriceRandom.advanceRows(startIndex);

            // generate information for initial order
            customerKey = generateCustomerKey();
            orderDate = orderDateRandom.nextValue();
            lineCount = lineCountRandom.nextValue() - 1;
        }

        @Override
        protected Lineorder computeNext()
        {
            if (index >= rowCount)
            {
                return endOfData();
            }

            Lineorder lineorder = makeLineorder(startIndex + index + 1, customerKey);
            lineNumber++;

            if (lineNumber > lineCount)
            {
                customerKeyRandom.rowFinished();
                clerkRandom.rowFinished();
                orderCommentRandom.rowFinished();
                lineitemCommentRandom.rowFinished();
                quantityRandom.rowFinished();
                discountRandom.rowFinished();
                taxRandom.rowFinished();
                linePartKeyRandom.rowFinished();
                supplierNumberRandom.rowFinished();
                shipDateRandom.rowFinished();
                commitDateRandom.rowFinished();
                receiptDateRandom.rowFinished();
                returnedFlagRandom.rowFinished();
                shipInstructionsRandom.rowFinished();
                shipModeRandom.rowFinished();
                totalPriceRandom.rowFinished();
                orderDateRandom.rowFinished();
                orderPriorityRandom.rowFinished();
                lineCountRandom.rowFinished();

                index++;

                // generate information for next order
                customerKey = generateCustomerKey();
                lineCount = lineCountRandom.nextValue() - 1;
                orderDate = orderDateRandom.nextValue();
                lineNumber = 0;
            }

            return lineorder;
        }

        private Lineorder makeLineorder(long orderIndex, long customerKey)
        {
            long orderKey = makeOrderKey(orderIndex);

            long partKey = linePartKeyRandom.nextValue();
            int supplierNumber = supplierNumberRandom.nextValue();
            long supplierKey = selectPartSupplier(partKey, supplierNumber, scaleFactor);

            int clerk = clerkRandom.nextValue();

            int quantity = quantityRandom.nextValue();
            int discount = discountRandom.nextValue();
            int tax = taxRandom.nextValue();

            long partPrice = calculatePartPrice(partKey);
            long extendedPrice = partPrice * quantity;

            int shipDate = shipDateRandom.nextValue();
            shipDate += orderDate;
            int commitDate = commitDateRandom.nextValue();
            commitDate += orderDate;
            int receiptDate = receiptDateRandom.nextValue();
            receiptDate += orderDate;

            String returnedFlag;
            if (GenerateUtils.isInPast(receiptDate))
            {
                returnedFlag = returnedFlagRandom.nextValue();
            }
            else
            {
                returnedFlag = "N";
            }

            char lineStatus;
            char orderStatus;
            if (GenerateUtils.isInPast(shipDate))
            {
                lineStatus = 'F';
                orderStatus = 'F';
            }
            else
            {
                lineStatus = 'O';
                orderStatus = 'O';
            }

            String shipInstructions = shipInstructionsRandom.nextValue();
            String shipMode = shipModeRandom.nextValue();
            String orderComment = orderCommentRandom.nextValue();
            String lineitemComment = lineitemCommentRandom.nextValue();

            return new Lineorder(
                    index,
                    orderKey,
                    customerKey,
                    orderStatus,
                    String.format(Locale.ENGLISH, "Clerk#%09d", clerk),
                    orderComment,
                    lineitemComment,
                    quantity,
                    discount,
                    tax,
                    partKey,
                    supplierKey,
                    shipDate,
                    commitDate,
                    receiptDate,
                    returnedFlag,
                    shipInstructions,
                    shipMode,
                    totalPriceRandom.nextValue(),
                    orderDate,
                    orderPriorityRandom.nextValue(),
                    0,
                    lineNumber + 1,
                    extendedPrice,
                    lineStatus,
                    System.currentTimeMillis());
        }

        private long generateCustomerKey()
        {
            long customerKey = customerKeyRandom.nextValue();
            int delta = 1;
            while (customerKey % CUSTOMER_MORTALITY == 0) {
                customerKey += delta;
                customerKey = Math.min(customerKey, maxCustomerKey);
                delta *= -1;
            }
            return customerKey;
        }
    }

    static RandomBoundedInt createQuantityRandom()
    {
        return new RandomBoundedInt(209208115, QUANTITY_MIN, QUANTITY_MAX, LINE_COUNT_MAX);
    }

    static RandomBoundedInt createDiscountRandom()
    {
        return new RandomBoundedInt(554590007, DISCOUNT_MIN, DISCOUNT_MAX, LINE_COUNT_MAX);
    }

    static RandomBoundedInt createTaxRandom()
    {
        return new RandomBoundedInt(721958466, TAX_MIN, TAX_MAX, LINE_COUNT_MAX);
    }

    static RandomBoundedLong createPartKeyRandom(double scaleFactor)
    {
        return new RandomBoundedLong(1808217256, scaleFactor >= 30000, PART_KEY_MIN, (long) (PartGenerator.SCALE_BASE * scaleFactor), LINE_COUNT_MAX);
    }

    static RandomBoundedInt createShipDateRandom()
    {
        return new RandomBoundedInt(1769349045, SHIP_DATE_MIN, SHIP_DATE_MAX, LINE_COUNT_MAX);
    }

    static RandomBoundedInt createLineCountRandom()
    {
        return new RandomBoundedInt(1434868289, LINE_COUNT_MIN, LINE_COUNT_MAX);
    }

    static RandomBoundedInt createOrderDateRandom()
    {
        return new RandomBoundedInt(1066728069, ORDER_DATE_MIN, ORDER_DATE_MAX);
    }



    static long selectPartSupplier(long partKey, long supplierNumber, double scaleFactor)
    {
        long supplierCount = (long) (SupplierGenerator.SCALE_BASE * scaleFactor);
        return ((partKey + (supplierNumber + ((supplierCount / 4) + ((partKey - 1) / supplierCount)))) % supplierCount) + 1;
    }

    static long calculatePartPrice(long p)
    {
        long price = 90000;

        // limit contribution to $200
        price += (p / 10) % 20001;
        price += (p % 1000) * 100;

        return (price);
    }

    static long makeOrderKey(long orderIndex)
    {
        long low_bits = orderIndex & ((1 << ORDER_KEY_SPARSE_KEEP) - 1);

        long ok = orderIndex;
        ok = ok >> ORDER_KEY_SPARSE_KEEP;
        ok = ok << ORDER_KEY_SPARSE_BITS;
        ok = ok << ORDER_KEY_SPARSE_KEEP;
        ok += low_bits;

        return ok;
    }
}
