package view;


import java.awt.*;

public class VerticalLayout implements LayoutManager {
    private Dimension size = new Dimension();
    private static final int DISTANCE_BETWEEN_COMPONENTS = 5;

    public void addLayoutComponent   (String name, Component comp) {}
    public void removeLayoutComponent(Component comp) {}

    public Dimension minimumLayoutSize(Container c) {
        return calculateBestSize(c);
    }
    public Dimension preferredLayoutSize(Container c) {
        return calculateBestSize(c);
    }

    //Метод располагает компоненты в контейнере
    public void layoutContainer(Container container) {
        Component[] list = container.getComponents();
        int currentY = DISTANCE_BETWEEN_COMPONENTS;
        for (int i = 0; i < list.length; i++) {
            Dimension pref = list[i].getPreferredSize();
            //Размещение компонента
            list[i].setBounds(DISTANCE_BETWEEN_COMPONENTS, currentY, pref.width, pref.height);

            //Вычисляем позицию для следующего компонента (между компонентами расстояние 5 пикселей)
            currentY += DISTANCE_BETWEEN_COMPONENTS;
            currentY += pref.height;
        }
    }

    //Вычисляет лучший размер контейнера следующим образом:
    //Ширина = Наибольшая ширина компонента + 5
    //Высота = Сумма высот компонент + DISTANCE_BETWEEN_COMPONENTS
    private Dimension calculateBestSize(Container c) {
        Component[] list = c.getComponents();
        int maxWidth = 0;
        for (int i = 0; i < list.length; i++) {
            int width = list[i].getWidth();
            if ( width > maxWidth )
                maxWidth = width;
        }
        size.width = maxWidth + 5;
        int height = 0;
        for (int i = 0; i < list.length; i++) {
            height += DISTANCE_BETWEEN_COMPONENTS;
            height += list[i].getHeight();
        }
        size.height = height;
        return size;
    }
}

/*public class VerticalLayout implements LayoutManager
{
    private Dimension size = new Dimension();

    // Следующие два метода не используются
    public void addLayoutComponent   (String name, Component comp) {}
    public void removeLayoutComponent(Component comp) {}

    // Метод определения минимального размера для контейнера
    public Dimension minimumLayoutSize(Container c) {
        return calculateBestSize(c);
    }
    // Метод определения предпочтительного размера для контейнера
    public Dimension preferredLayoutSize(Container c) {
        return calculateBestSize(c);
    }
    // Метод расположения компонентов в контейнере
    public void layoutContainer(Container container)
    {
        // Список компонентов
        Component list[] = container.getComponents();
        int currentY = 5;
        for (int i = 0; i < list.length; i++) {
            // Определение предпочтительного размера компонента
            Dimension pref = list[i].getPreferredSize();
            // Размещение компонента на экране
            list[i].setBounds(5, currentY, pref.width, pref.height);
            // Учитываем промежуток в 5 пикселов
            currentY += 5;
            // Смещаем вертикальную позицию компонента
            currentY += pref.height;
        }
    }
    // Метод вычисления оптимального размера контейнера
    private Dimension calculateBestSize(Container c)
    {
        // Вычисление длины контейнера
        Component[] list = c.getComponents();
        int maxWidth = 0;
        for (int i = 0; i < list.length; i++) {
            int width = list[i].getWidth();
            // Поиск компонента с максимальной длиной
            if ( width > maxWidth )
                maxWidth = width;
        }
        // Размер контейнера в длину с учетом левого отступа
        size.width = maxWidth + 5;
        // Вычисление высоты контейнера
        int height = 0;
        for (int i = 0; i < list.length; i++) {
            height += 5;
            height += list[i].getHeight();
        }
        size.height = height;
        return size;
    }
}*/

