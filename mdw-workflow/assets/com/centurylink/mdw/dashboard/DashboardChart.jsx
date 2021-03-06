import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Doughnut, Bar, Line} from '../node/node_modules/react-chartjs-2';
import constants from '../react/constants';
import {shade} from '../react/statuses';
import ChartHeader from './ChartHeader.jsx';
import ChartLegend from './ChartLegend.jsx';

class DashboardChart extends Component {

  constructor(...args) {
    super(...args);
    this.initialSelect = 5;
    this.maxTops = 50;
    this.defaultTimespan = 'Week';
    this.chartColors = ['#3366CC','#FF9900','#DC3912','#109618','#990099','#3B3EAC','#0099C6','#DD4477','#66AA00','#B82E2E','#316395','#994499','#22AA99','#AAAA11','#6633CC','#E67300','#8B0707','#329262','#5574A6','#3B3EAC'];
    this.chartOptions = {legend: {display: false, position: 'bottom'}};
    this.minute_ms = 60 * 1000;
    this.hour_ms = 60 * this.minute_ms;
    this.day_ms = 24 * this.hour_ms;

    this.state = {
      timespan: sessionStorage.getItem("mdwDashboardTimespan") || this.defaultTimespan,
      breakdown: this.props.breakdownConfig.breakdowns[0].name,
      tops: [],
      selected: [],
      filters: this.props.breakdownConfig.filters || {},
      data: {}
    };
    if (this.state.filters.Ending) {
      this.state.filters.Ending = this.getDefaultEnd(this.defaultTimespan);
    }
    this.getBreakdown = this.getBreakdown.bind(this);
    this.getDefaultEnd = this.getDefaultEnd.bind(this);
    this.getStart = this.getStart.bind(this);
    this.setTops = this.setTops.bind(this);
    this.setSelected = this.setSelected.bind(this);
    this.handleTimespanChange = this.handleTimespanChange.bind(this);
    this.handleBreakdownChange = this.handleBreakdownChange.bind(this);
    this.handleTopSelect = this.handleTopSelect.bind(this);
    this.handleSelectCancel = this.handleSelectCancel.bind(this);
    this.handleSelectApply = this.handleSelectApply.bind(this);
    this.handleFilterChange = this.handleFilterChange.bind(this);
    this.handleFilterReset = this.handleFilterReset.bind(this);
    this.isDefaultFilters = this.isDefaultFilters.bind(this);
    this.handleDownload = this.handleDownload.bind(this);
    this.buildUrl = this.buildUrl.bind(this);
    this.update = this.update.bind(this);
    this.updateChart = this.updateChart.bind(this);
    this.retrieveTops = this.retrieveTops.bind(this);
    this.retrieveData = this.retrieveData.bind(this);
    this.getChartOptions = this.getChartOptions.bind(this);
    this.getSummaryChartOptions = this.getSummaryChartOptions.bind(this);
    this.getChartColors = this.getChartColors.bind(this);
    this.handleOverviewDataClick = this.handleOverviewDataClick.bind(this);
    this.handleMainDataClick = this.handleMainDataClick.bind(this);
  }

  // selected breakdown object from breakdownConfig
  getBreakdown() {
    return this.props.breakdownConfig.breakdowns.find(bd => bd.name === this.state.breakdown);
  }

  getDefaultEnd(timespan) {
    var end = new Date();
    end.setSeconds(0);
    end.setMilliseconds(0);
    if (timespan === 'Day') {
      end.setMinutes(0);
    }
    if (timespan === 'Week' || timespan === 'Month') {
      end.setMinutes(0);
      end.setHours(0);
    }
    return end;
  }

  getStart() {
    if (this.state.filters.Ending) {
      var spanMs;
      if (this.state.timespan === 'Hour') {
        spanMs = this.minute_ms * 59;
      }
      else if (this.state.timespan === 'Day') {
        spanMs = this.hour_ms * 23;
      }
      else if (this.state.timespan === 'Week') {
        spanMs = this.day_ms * 6;
      }
      else if (this.state.timespan === 'Month') {
        spanMs = this.day_ms * 29;
      }
      return new Date(this.state.filters.Ending.getTime() - spanMs);
    }
  }

  setTops(tops, selected) {
    return new Promise(resolve => {
      this.setState({
        timespan: this.state.timespan,
        breakdown: this.state.breakdown,
        tops: tops,
        selected: selected ? selected : this.state.selected,
        filters: this.state.filters,
        data: {}
      }, () => {
        if (selected) {
          this.previousSelected = this.state.selected;
        }
        resolve();
      });
    });
  }

  setSelected(selected) {
    return new Promise(resolve => {
      this.setState({
        timespan: this.state.timespan,
        breakdown: this.state.breakdown,
        tops: this.state.tops,
        selected: selected,
        filters: this.state.filters,
        data: {}
      }, () => {
        resolve();
      });
    });
  }

  handleTimespanChange(timespan) {
    const filters = Object.assign({}, this.state.filters, {
      Ending: this.getDefaultEnd(timespan),
    });
    sessionStorage.setItem("mdwDashboardTimespan", timespan);
    this.setState({
      timespan: timespan,
      breakdown: this.state.breakdown,
      tops: [],
      selected: [],
      filters: filters,
      data: {}
    }, this.update);
  }

  handleBreakdownChange(breakdown) {
    this.setState({
      timespan: this.state.timespan,
      breakdown: breakdown,
      tops: [],
      selected: [],
      filters: this.state.filters,
      data: {}
    }, this.update);
  }

  handleTopSelect(top, isSelected) {
    var selected = this.state.selected.slice(0);
    if (isSelected) {
      selected.push(top);
    }
    else {
      const idx = selected.findIndex(t => t.id === top.id);
      selected.splice(idx, 1);
    }
    this.setSelected(selected);
  }

  handleSelectCancel() {
    if (this.previousSelected) {
      this.setSelected(this.previousSelected, this.updateChart());
    }
  }

  handleSelectApply() {
    this.updateChart();
  }

  handleFilterChange(filters) {
    if (!filters.Ending) {
      filters.Ending = this.getDefaultEnd(this.state.timespan);
    }
    if (this.state.timespan === 'Hour' || this.state.timespan === 'Day') {
      filters.Ending.setHours(new Date().getHours());
    }
    else {
      // midnight of the next day
      filters.Ending.setHours(0);
    }
    this.setState({
      timespan: this.state.timespan,
      breakdown: this.state.breakdown,
      tops: [],
      selected: [],
      filters: filters,
      data: {}
    }, this.update);
  }

  handleFilterReset() {
    var filters = this.props.breakdownConfig.filters || {};
    if (filters.Ending) {
      filters.Ending = this.getDefaultEnd(this.state.timespan);
    }
    this.handleFilterChange(filters);
  }

  isDefaultFilters() {
    const defaultFilters = this.props.breakdownConfig.filters || {};
    if (defaultFilters.Ending) {
      defaultFilters.Ending = this.getDefaultEnd(this.defaultTimespan);
    }
    for (let key of Object.keys(this.state.filters)) {
      if (!this.state.filters[key] && !defaultFilters[key]) {
        continue;
      }
      if (this.state.filters[key].getTime) {
        if (!defaultFilters[key].getTime || (this.state.filters[key].getTime() !== defaultFilters[key].getTime())) {
          return false;
        }
      }
      else if (this.state.filters[key] !== defaultFilters[key]) {
        return false;
      }
    }
    return true;
  }

  handleDownload() {
    const breakdown = this.getBreakdown();
    if (breakdown) {
      var downloadUrl = this.buildUrl(this.context.serviceRoot + breakdown.data);
      var sep = downloadUrl.indexOf('?') >= 0 ? '&' : '?';
      if (breakdown.instancesParam) {
        downloadUrl += sep + breakdown.instancesParam + '=%5B' + this.state.selected.map(sel => {
          return breakdown.selectField ? encodeURI(sel[breakdown.selectField]) : encodeURI(sel.id);
        }).join() + '%5D';
      }
      sep = downloadUrl.indexOf('?') >= 0 ? '&' : '?';
      location = downloadUrl + sep + 'DownloadFormat=xlsx&NoPersistence=true';
    }
  }

  componentDidMount() {
    this.update();
  }

  buildUrl(base) {
    const breakdown = this.getBreakdown();
    var url = base + (breakdown.data.indexOf('?') >= 0 ? '&' : '?');
    let start = this.getStart();
    if (start) {
      url += 'Starting=' + start.toISOString();
    }
    if (this.state.timespan === 'Hour') {
      url += '&Increment=minute';
    }
    else if (this.state.timespan === 'Day') {
      url += '&Increment=hour';
    }
    Object.keys(this.state.filters).forEach(key => {
      let val = this.state.filters[key];
      if (val) {
        if (val instanceof Date) {
          if (key == 'Ending') {
            if (this.state.timespan === 'Hour') {
              val = new Date(val.getTime() + this.minute_ms);
            }
            else if (this.state.timespan === 'Day') {
              val = new Date(val.getTime() + this.hour_ms);
            }
            else {
              // midnight of the day after the selected date
              val = new Date(val.getTime() + this.day_ms);
            }
          }
          url += '&' + key + '=' + val.toISOString();
        }
        else {
          url += '&' + key + '=' + encodeURIComponent(val);
        }
      }
    });
    if (url.endsWith('?')) {
      url = url.substring(0, url.length - 1);
    }
    return url;
  }

  // retrieves tops, resets selected, then retrieves data and updates state
  update() {
    this.retrieveTops()
    .then(tops => {
      this.setTops(tops, tops.slice(0, (this.initialSelect > tops.length ? tops.length : this.initialSelect)));
    })
    .then(() => {
      this.updateChart();
    });
  }

  updateChart() {
    this.retrieveData()
    .then(data => {
      this.acceptData(data);
    })
    .then(() => {
      const breakdown = this.getBreakdown();
      if (breakdown.websocketUrl) {
        if (this.websocket) {
          this.websocket.close();
        }
        this.websocket = new WebSocket(breakdown.websocketUrl);
        this.websocket.onopen = () => {
          this.websocket.send(breakdown.data);
        };
        this.websocket.onmessage = event => {
          const data = JSON.parse(event.data);
          this.acceptData(data)
          .then(() => {
            this.recalcSummary(data);
          });
        };
      }
    });
  }

  // works for Metrics-style data (every id/name represented in every item)
  recalcSummary(data) {
    var accum = {};
    for (let t in data) {
      let items = data[t];
      for (let item of items) {
        let metric = accum[item.name];
        if (metric) {
          metric.value += item.value;
        }
        else {
          accum[item.name] = { id: item.id, name: item.name, value: item.value };
        }
      }
    }
    var avgs = [];
    for (let a in accum) {
      avgs.push({
        id: accum[a].id,
        name: accum[a].name,
        value: Math.round(accum[a].value / Object.keys(data).length)
      });
    }
    this.setState({
      timespan: this.state.timespan,
      breakdown: this.state.breakdown,
      tops: avgs,
      selected: avgs,
      filters: this.state.filters,
      data: data
    });
  }

  // updates state and returns a promise
  retrieveTops() {
    return new Promise(resolve => {
      const breakdown = this.getBreakdown();
      if (breakdown && breakdown.tops) {
        $mdwUi.hubLoading(true);
        var topsUrl = this.buildUrl(this.context.serviceRoot + breakdown.tops);
        fetch(new Request(topsUrl, {
          method: 'GET',
          headers: { Accept: 'application/json', 'mdw-app-id': 'mdw-hub' },
          credentials: 'same-origin'
        }))
        .then(response => {
          return response.json();
        })
        .then(tops => {
          $mdwUi.hubLoading(false);
          resolve(tops);
        });
      }
      else {
        resolve([]);
      }
    });
  }

  retrieveData() {
    return new Promise(resolve => {
      const breakdown = this.getBreakdown();
      if (breakdown) {
        $mdwUi.hubLoading(true);
        var dataUrl = this.buildUrl(this.context.serviceRoot + breakdown.data);
        if (breakdown.instancesParam) {
          dataUrl += '&' + breakdown.instancesParam + '=%5B' + this.state.selected.map(sel => {
            return breakdown.selectField ? encodeURI(sel[breakdown.selectField]) : encodeURI(sel.id);
          }).join() + '%5D';
        }
        fetch(new Request(dataUrl, {
          method: 'GET',
          headers: { Accept: 'application/json', 'mdw-app-id': 'mdw-hub' },
          credentials: 'same-origin'
        }))
        .then(response => {
          this.hostname = response.headers.get('mdw-hostname');
          return response.json();
        })
        .then(data => {
          $mdwUi.hubLoading(false);
          resolve(data);
        });
      }
      else {
        resolve({});
      }
    });
  }

  acceptData(data) {
    return new Promise(resolve => {
      this.setState({
        timespan: this.state.timespan,
        breakdown: this.state.breakdown,
        tops: this.state.tops,
        selected: this.state.selected,
        filters: this.state.filters,
        data: data
      }, () => resolve());
    });
  }

  getChartOptions() {
    const breakdown = this.getBreakdown();
    if (breakdown.chartOptions) {
      var options = Object.assign({}, this.chartOptions, breakdown.chartOptions);
      if (this.hostname && options.scales && options.scales.xAxes && options.scales.xAxes.length > 0
            && options.scales.xAxes[0].scaleLabel) {
        const scaleLabel = options.scales.xAxes[0].scaleLabel;
        const labelString = scaleLabel.labelString;
        if (labelString && labelString.indexOf('${hostname}') >= 0) {
          scaleLabel.labelString = labelString.replace('${hostname}', this.hostname);
        }
      }
      return options;
    }
    return this.chartOptions;
  }

  getSummaryChartOptions() {
    const breakdown = this.getBreakdown();
    if (breakdown.summaryChartOptions) {
      return Object.assign({}, this.chartOptions, breakdown.summaryChartOptions);
    }
    else {
      return this.chartOptions;
    }
  }

  getChartColors() {
    const breakdown = this.getBreakdown();
    if (Array.isArray(breakdown.colors)) {
      return breakdown.colors;
    }
    else if (typeof breakdown.colors === 'function') {
      return breakdown.colors(this.state.selected);
    }
    else {
      return this.chartColors;
    }
  }

  getOverviewData() {
    const breakdown = this.getBreakdown();
    const chartColors = this.getChartColors();
    const chartOptions = this.getChartOptions();
    const overallData = {labels: [], datasets: [{label: 'Overall', data: [], backgroundColor: []}]};
    var max, total = 0;
    if (breakdown.stacked && chartOptions.scales && chartOptions.scales.yAxes && chartOptions.scales.yAxes.length > 0) {
      const ticks = chartOptions.scales.yAxes[0].ticks;
      if (ticks) {
        max = ticks.max;
      }
    }
    this.state.selected.forEach((sel, i) => {
      let label = breakdown.summaryChart === 'bar' ? '' : sel.name;
      overallData.labels.push(label);
      overallData.datasets[0].data.push(sel.value);
      total += sel.value;
      overallData.datasets[0].backgroundColor.push(chartColors[i]);
    }, this);
    if (max) {
      // fill up donut
      overallData.labels.push('');
      overallData.datasets[0].data.push(max - total);
    }
    return overallData;
  }

  getLabel(date) {
    const now = new Date();
    let lab = date.getFullYear() === now.getFullYear() ? '' : date.getFullYear() + ' ';
    if (this.state.timespan === 'Hour' || this.state.timespan === 'Day') {
      if (date.getMonth() !== now.getMonth() || date.getDate() !== now.getDate()) {
        lab += constants.months[date.getMonth()] + ' ' + date.getDate() + ' ';
      }
      lab += date.getHours() + ':' + (date.getMinutes() < 10 ? '0' + date.getMinutes() : date.getMinutes());
    }
    else {
      lab += constants.months[date.getMonth()] + ' ' + date.getDate();
    }
    return lab;
  }

  getMainData() {
    const breakdown = this.getBreakdown();
    const lineData = {labels: [], datasets: []};
    var datasets = {}; // id to dataset
    const chartColors = this.getChartColors();
    if (this.state.selected.length > 0) {
      this.state.selected.forEach((sel, i) => {
        let fill = breakdown.fill && (!Array.isArray(breakdown.fill) || breakdown.fill[i]);
        let dataset = {label: sel.name, borderColor: chartColors[i], data: [], fill: fill ? fill : false};
        if (fill && chartColors[i]) {
          dataset.backgroundColor = shade(chartColors[i], 0.5);
        }
        datasets[sel.id] = dataset;
        lineData.datasets.push(dataset);
        Object.keys(this.state.data).forEach((key, j) => {
          if (i === 0) {
            lineData.labels.push(key.endsWith('00Z') ? this.getLabel(new Date(key)) : key);
          }
          const aggs = this.state.data[key];
          const selAgg = aggs.find(agg => agg.id === sel.id);
          var value = selAgg ? selAgg.value : 0;
          let stacked = breakdown.stacked && (!Array.isArray(breakdown.stacked) || breakdown.stacked[i]);
          if (stacked && i > 0) {
            value += lineData.datasets[i-1].data[j]; // additive
          }
          dataset.data.push(value);
        }, this);
      }, this);
    }
    else { // eg: total
      let dataset = {borderColor: chartColors[0], data: [], fill: breakdown.fill ? breakdown.fill : false};
      lineData.datasets.push(dataset);
      Object.keys(this.state.data).forEach(key => {
        lineData.labels.push(key.endsWith('00Z') ? this.getLabel(new Date(key)) : key);
        let point = this.state.data[key][0];
        if (point) {
          dataset.data.push(point.value);
        }
        else {
          dataset.data.push(0);
        }
      }, this);
    }
    return lineData;
  }

  handleOverviewDataClick(chartElements) {
    if (chartElements && chartElements.length > 0) {
      if (this.props.onOverviewDataClick) {
        const selection = this.state.selected[chartElements[0]._index];
        const filters = JSON.parse(JSON.stringify(this.state.filters));
        filters.Starting = this.getStart();
        this.props.onOverviewDataClick(this.getBreakdown().name, selection, filters);
      }
    }
  }

  handleMainDataClick(chartElements) {
    if (chartElements && chartElements.length === 1) {
      // console.log("DATASET INDEX: " + chartElements[0]._datasetIndex);
    }
  }

  render() {
    const breakdown = this.getBreakdown();
    const overviewData = this.getOverviewData();
    const mainData = this.getMainData();
    const topsLoading = breakdown.tops && !this.state.tops.length;

    return (
      <div className="panel panel-default mdw-panel" style={{border:'none'}}>
        <ChartHeader title={this.props.title}
          breakdownConfig={this.props.breakdownConfig}
          breakdown={this.state.breakdown}
          timespan={this.state.timespan}
          list={this.props.list}
          tops={this.state.tops}
          selected={this.state.selected}
          filters={this.state.filters}
          filterOptions={this.props.breakdownConfig.filterOptions}
          onTimespanChange={this.handleTimespanChange}
          onBreakdownChange={this.handleBreakdownChange}
          onSelect={this.handleTopSelect}
          onSelectCancel={this.handleSelectCancel}
          onSelectApply={this.handleSelectApply}
          onFilterChange={this.handleFilterChange}
          onFilterReset={this.handleFilterReset}
          isDefaultFilters={this.isDefaultFilters()}
          onDownload={this.handleDownload}/>
        <div className="mdw-section" style={{display:'flex'}}>
          {topsLoading &&
            <div className="mdw-chart-title" style={{minWidth:'250px'}}>
            </div>
          }
          {breakdown.tops && !topsLoading &&
            <div style={{maxWidth:'282px',maxHeight:'282px'}}>
              {(!breakdown.summaryChart || breakdown.summaryChart === 'donut') &&
                <Doughnut
                  data={overviewData}
                  options={this.getSummaryChartOptions()}
                  width={250} height={250}
                  getElementAtEvent={this.handleOverviewDataClick} />
              }
              {breakdown.summaryChart === 'bar' &&
                <Bar
                  data={overviewData}
                  options={this.getSummaryChartOptions()}
                  width={250} height={250}
                  getElementAtEvent={this.handleOverviewDataClick} />
              }
              {breakdown.summaryTitle &&
                <div className="mdw-chart-subtitle" style={{width:'250px'}}>
                  {breakdown.summaryTitle}
                </div>
              }
              <ChartLegend
                colors={this.getChartColors()}
                items={this.state.selected} />
            </div>
          }
          {!breakdown.tops && !topsLoading &&
            <div className="mdw-chart-title" style={{width:'250px'}}>
              {breakdown.name}
            </div>
          }
          <div style={{height:'100%',width:'100%'}}>
            <Line
              data={mainData}
              options={this.getChartOptions()}
              getElementAtEvent={this.handleMainDataClick} />
          </div>
        </div>
      </div>
    );
  }
}

DashboardChart.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default DashboardChart;
